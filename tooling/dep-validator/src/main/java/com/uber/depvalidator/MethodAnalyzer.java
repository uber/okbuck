package com.uber.depvalidator;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.CodeScanner;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.strings.Atom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Implements an analysis to look for methods that went missing as a result of a third-party library
 * dependency change.
 */
public class MethodAnalyzer {

  private static final int NUM_HITS = 10;

  private static final Atom INIT_ATOM = Atom.findOrCreateAsciiAtom("<init>");

  private static final TypeName OBJECT_TYPE_NAME = TypeName.findOrCreate("Ljava/lang/Object");

  private final Analyzer analyzer;

  private final Set<MethodReference> traversalStops;

  private final Set<Integer> callChainHashes;

  /**
   * If we want to share the traversal tracker across different targets, we need to make sure that
   * the "allocation set" (understood as a union of root allocations and path allocations) never
   * "looses" any discovered allocation (it only grows). Otherwise, the analysis may miss some call
   * paths leading to missing functions, as the decision on whether to analyze a given call site
   * again is driven by the size of the "allocation set" that has been recorded last time this call
   * site was analyzed.
   *
   * <p>As an example, consider a call to method foo(). The first time (when analyzing some target)
   * it is encountered by the analysis, the only class in the "allocation set" is BBB. It is then
   * recorded that when analyzing this call site, the size of the "allocation set" was 1.
   *
   * <p>Now consider that before analyzing another target, class AAA was removed from the
   * "allocation set". Then, when analyzing this new target, a call to method foo() is encountered
   * again, but this time the only class in the "allocation set" is ZZZ. Since the current size of
   * the allocation set is 1 and last time a call to method foo() was encountered its size was also
   * 1, then the analysis has no reason to suspect that a call to method foo() needs to be processes
   * again. This, however, would be wrong, as processing foo() again could lead to discovering a
   * missing method via calls to methods of class ZZZ which have so far remained unexplored.
   *
   * <p>On the flip side, keeping all classes in the "allocation set" when analyzing multiple
   * targets may lead to false positives. In the example above, we will keep both AAA and ZZZ
   * classes when analyzing the second target even though AAA may have never be encountered when
   * analyzing this target separately.
   */
  private final Map<MethodInfo, TraversalInfo> traversalTracker;

  private final Set<TypeReference> allocations;

  /**
   * Constructs the missing method analyzer.
   *
   * @param analyzer a reference to the main analyzer object
   */
  public MethodAnalyzer(Analyzer analyzer) {
    this.analyzer = analyzer;
    this.traversalStops = new HashSet<>();
    this.callChainHashes = new HashSet<>();
    this.traversalTracker = new HashMap<>();
    this.allocations = new HashSet<>();
  }

  /**
   * Reports missin method information, if any.
   *
   * @param foundInfo result of the missing method traversal
   * @param target build target (service) where the missing method is searched (used for reporting
   *     purposes)
   * @param firstHit determines if it is the first time a missing class has been discovered
   * @param entryMethod the entry method starting the missing method call chain
   * @param callSite the first call site (in the entry method) starting the missing method call
   *     chain
   * @param chaPre class hierarchy for pre-commit service fat jar
   * @param preTargetCache cache for method calling targets for pre-commit version of the code (used
   *     for call path validation)
   * @param a value determining if an actual non-redundant missing method call chain has been
   *     reported
   * @param rootClasses post-commit classes implementing logic of a given service whose methods are
   *     to be used as starting points for call chain discovery
   * @param extClassJarInfos a map from post-commit third-party library classes to jar file info
   *     string (hash and path) for a jar defining these classes
   */
  private boolean reportMissingMethod(
      TraversalInfo foundInfo,
      String target,
      boolean firstHit,
      IMethod entryMethod,
      CallSiteReference callSite,
      @Nullable ClassHierarchy chaPre,
      @Nullable Map<CallSiteReference, Set<IMethod>> preTargetCache,
      Set<TypeName> rootClasses,
      Map<TypeName, String> extClassJarInfos)
      throws Exception {
    List<MethodReference> callChain = new ArrayList<>();
    List<Integer> callChainLineNumbers = new ArrayList<>();

    foundInfo.state = TraversalState.INITIAL;

    callChain.add(foundInfo.targetMethod);
    while (true) {
      foundInfo = foundInfo.prevTraversalInfo;
      if (foundInfo == null) {
        break;
      } else {
        callChainLineNumbers.add(foundInfo.lineNumber);
        callChain.add(foundInfo.targetMethod);
      }
    }
    if (callChain.size() > 1) {
      // make sure we are reporting only chains that are longer than 1
      // (otherwise would be caught at build time and is therefore likely a false
      // alarm)
      int callChainHash =
          analyzer.getExtPathHash(
              callChain,
              (m) -> {
                return m.getDeclaringClass().getName();
              },
              rootClasses);
      if (!callChainHashes.contains(callChainHash)
          && postChainOnly(
              Collections.singleton(callSite),
              callChain,
              callChain.size() - 1,
              chaPre,
              preTargetCache)) {
        // only report unique paths
        callChainHashes.add(callChainHash);
        if (firstHit) {
          System.out.println(
              "MISSING CALL PATHS FOUND" + (target == null ? "" : " FOR " + target) + ":");
        }
        if (analyzer.input.verbose) {
          TypeName declaringClass = entryMethod.getDeclaringClass().getName();
          System.out.println(
              "\t"
                  + Analyzer.getClassNameString(entryMethod.getDeclaringClass().getName())
                  + "."
                  + entryMethod.getName()
                  + "():"
                  + entryMethod.getLineNumber(callSite.getProgramCounter()));
          for (int i = callChain.size() - 1; i >= 0; i--) {
            MethodReference methodRef = callChain.get(i);
            declaringClass = methodRef.getDeclaringClass().getName();
            if (i > 0) {
              System.out.println(
                  "\t"
                      + Analyzer.getClassNameString(methodRef.getDeclaringClass().getName())
                      + "."
                      + methodRef.getName()
                      + "():"
                      + callChainLineNumbers.get(i - 1)
                      + " ["
                      + Analyzer.extractJarFileName(getJarInfo(methodRef, extClassJarInfos))
                      + "]");
            } else {
              System.out.println(
                  "\t"
                      + Analyzer.getClassNameString(methodRef.getDeclaringClass().getName())
                      + "."
                      + methodRef.getName()
                      + "()");
            }
          }
          System.out.println("");
        } else {
          MethodReference methodRef = callChain.get(0);
          System.out.println(
              "\t"
                  + Analyzer.getClassNameString(methodRef.getDeclaringClass().getName())
                  + "."
                  + methodRef.getName()
                  + "()");
        }
        return true;
      }
    }
    return false;
  }

  /**
   * Performs a reachability analysis of the post-commit service fat jar starting with a set of
   * service's root methods to look for methods that went missing as a result of a third-party
   * library dependency change. The reachability analysis is implemented using a simplified version
   * of the RTA algorithm typically used for call graph construction (see "Fast static analysis of
   * C++ virtual function calls" by David F. Bacon and Peter F. Sweeney).
   *
   * @param numFound number of missing methods found so far across all targets
   * @param target name of the target (aka service) being analyzed
   * @param additionalReferencedClasses classes transitively derived from classes listed in fat
   *     jar's metadata
   * @return <code>true</code> if any missing methods have been discovered; <code>false</code>
   *     otherwise
   * @param cha class hierarchy for post-commit service fat jar
   * @param chaPre class hierarchy for pre-commit service fat jar
   * @param rootClasses post-commit classes implementing logic of a given service whose methods are
   *     to be used as starting points for call chain discovery
   * @param extClassJarInfos a map from post-commit third-party library classes to jar file info
   *     string (hash and path) for a jar defining these classes
   * @param preExtClasses pre-commit third-party library classes
   * @param analysisStartTime time the entire analysis for this target has started
   * @return <code>true</code> number of missing methods found so far across all targets, inluding
   *     missing methods found during this analyze method call
   */
  public int analyze(
      int numFound,
      String target,
      Set<TypeName> additionalReferencedClasses,
      ClassHierarchy cha,
      @Nullable ClassHierarchy chaPre,
      Set<TypeName> rootClasses,
      Map<TypeName, String> extClassJarInfos,
      Set<TypeName> preExtClasses,
      long analysisStartTime)
      throws Exception {
    IClassLoader appLoader = cha.getLoader(ClassLoaderReference.Application);
    Map<IMethod, Set<CallSiteReference>> entrypoints = makeEntryPoints(rootClasses, cha, appLoader);

    for (TypeName className : additionalReferencedClasses) {
      // include "allocations" for classes in the metadata (and their
      // references) as they can be instantiated reflectively and,
      // consequently, will not be "seen" by the analysis looking for
      // explicit allocations
      addClassIfInScope(appLoader, className, allocations);
    }

    Map<CallSiteReference, Set<IMethod>> preTargetCache = null;
    if (chaPre != null) {
      preTargetCache = new HashMap<>();
    }

    Map<CallSiteReference, Set<IMethod>> postTargetCache = new HashMap<>();

    int numAllocations = allocations.size();
    boolean timedOut = false;
    for (Map.Entry<IMethod, Set<CallSiteReference>> e : entrypoints.entrySet()) {
      IMethod entryMethod = e.getKey();
      while (true) {
        // iterate until set of allocations does not grow anymore
        Set<CallSiteReference> callSites = e.getValue();
        for (CallSiteReference site : callSites) {
          TraversalInfo foundInfo = null;
          Set<IMethod> targets = getPossibleTargets(site, cha, postTargetCache);
          IMethod foundTarget = null;
          for (IMethod t : targets) {
            boolean repeat = true;
            while (repeat) {
              // under normal circumstances, this while loop should execute only once
              // and re-execute in the event of a call chain computation restart
              repeat = false;

              // check for timeout only at the top level - should be
              // sufficient and at the same time overhead will be low
              if (System.currentTimeMillis() - analysisStartTime > analyzer.input.timeoutMs) {
                timedOut = true;
                break;
              }

              TypeReference classRef = t.getDeclaringClass().getReference();
              if (t.isStatic() || t.isClinit() || allocations.contains(classRef)) {
                foundInfo =
                    findMissingMethods(
                        t,
                        site.getDeclaredTarget(),
                        appLoader,
                        null,
                        cha,
                        postTargetCache,
                        extClassJarInfos,
                        preExtClasses,
                        0);
                if (foundInfo == null) {
                  continue;
                }
                if (foundInfo.state == TraversalState.RESTART) {
                  repeat = true;
                  continue;
                }
                if (foundInfo.state == TraversalState.FOUND) {
                  numFound +=
                      reportMissingMethod(
                              foundInfo,
                              target,
                              numFound == 0,
                              entryMethod,
                              site,
                              chaPre,
                              preTargetCache,
                              rootClasses,
                              extClassJarInfos)
                          ? 1
                          : 0;
                  if (numFound >= analyzer.input.reportedProblems || timedOut) {
                    break;
                  }
                }
              }
            }
          }
          if (numFound >= analyzer.input.reportedProblems || timedOut) {
            break;
          }
        }
        if (allocations.size() == numAllocations || numFound >= analyzer.input.reportedProblems) {
          // we break out of while if no new allocated classes found or
          // we found all we have been looking for
          break;
        } else {
          numAllocations = allocations.size();
        }
      }
      if (numFound >= analyzer.input.reportedProblems || timedOut) {
        break;
      }
    }
    return numFound;
  }

  private void addClassIfInScope(
      IClassLoader appLoader, TypeName className, Set<TypeReference> classSet) {
    IClass c = appLoader.lookupClass(className);
    if (c != null && Analyzer.isApplicationClass(className.getPackage())) {
      classSet.add(c.getReference());
    }
  }

  private String getJarInfo(MethodReference methodRef, Map<TypeName, String> extClassJarInfos) {
    String jarInfo = extClassJarInfos.get(methodRef.getDeclaringClass().getName());
    if (jarInfo != null) {
      return jarInfo;
    } else {
      // don't have jar file info but since this is used only to check if the entire callchain
      // belongs to the same jar, it's OK to put a dummy unique info (per method)
      return "unknown " + methodRef.toString();
    }
  }

  // method references are canonicalized and can be compared with ==
  @SuppressWarnings("ReferenceEquality")
  /**
   * Checks if a given call chain to a missing method discovered in the post-commit code can also be
   * found in pre-commit code. By default we only want to report missing methods that are a direct
   * result of dependency change in a given commit.
   *
   * @param callSites a set of call sites to consider when tracing the call chain in pre-commit code
   * @param callChain the call chain discovered in post-commit code
   * @param callPos position in the call chain being trace (starting from the "top of the stack")
   * @param chaPre class hierarchy for pre-commit service fat jar
   * @param preTargetCache a cache for a given call site's call targets
   * @return <code>true</code> if a given call chain is only present in post-commit code; <code>
   *     false</code> otherwise
   */
  private boolean postChainOnly(
      Collection<CallSiteReference> callSites,
      List<MethodReference> callChain,
      int callPos,
      @Nullable ClassHierarchy chaPre,
      @Nullable Map<CallSiteReference, Set<IMethod>> preTargetCache)
      throws Exception {
    if (chaPre == null || preTargetCache == null) {
      return true;
    } else {
      IClassLoader appLoader = chaPre.getLoader(ClassLoaderReference.Application);
      for (CallSiteReference site : callSites) {
        if (callPos == 0) {
          // we reached the "missing" call level
          MethodReference siteMethodRef = site.getDeclaredTarget();
          TypeName siteClassName = siteMethodRef.getDeclaringClass().getName();
          if (siteMethodRef == callChain.get(callPos)
              && isMissingScopeMethod(siteMethodRef, siteClassName, appLoader)) {
            // found missing call
            return false;
          }
        } else {
          Set<IMethod> targets = getPossibleTargets(site, chaPre, preTargetCache);
          for (IMethod t : targets) {
            if (t.getReference() == callChain.get(callPos)) {
              // found next call in the chain
              return postChainOnly(
                  CodeScanner.getCallSites(t), callChain, callPos - 1, chaPre, preTargetCache);
            }
          }
        }
      }
      return true;
    }
  }

  // atoms are canonicalized and can be compared with ==
  @SuppressWarnings("ReferenceEquality")
  private boolean isMissingScopeMethod(
      MethodReference methodRef, TypeName className, IClassLoader appLoader) {
    IClass c = appLoader.lookupClass(className);
    if (c == null) {
      return true;
    } else if (Analyzer.isApplicationClass(c.getName().getPackage())) {
      IMethod m = c.getMethod(methodRef.getSelector());
      if (m == null) {
        // if a method is missing from a class that implements interface
        // containing this method, assume that it's false alarm
        Collection<IClass> ifaces = c.getAllImplementedInterfaces();
        for (IClass i : ifaces) {
          if (i.getMethod(methodRef.getSelector()) != null) {
            return false;
          }
        }
        return true;
      } else if (methodRef.getName() == INIT_ATOM) {
        if (m.getDeclaringClass().getName() != className) {
          // ICLass.getName() returns TypeName which is canonicalized
          // so using != for comparison is OK
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Updates information about when call chain exploration should stop. The idea is to stop
   * exploring a given call chain if it contains too many DECLARED calls that are the same (e.g.
   * calls to the same interface method) as this is typically a sign of a bogus call chain (e.g.
   * exploring a large list of auto-generated methods).
   *
   * @return <code>true</code> if any new traversal stops have been added; <code>false</code>
   *     otherwise
   */
  private boolean updateTraversalStops(TraversalInfo traversalInfo, int callChainLen) {
    HashMap<MethodReference, Integer> refCount = new HashMap<>();
    TraversalInfo prevInfo = traversalInfo.prevTraversalInfo;
    while (prevInfo != null) {
      MethodReference prevRef = prevInfo.declaredMethod;
      Integer count = refCount.get(prevRef);
      if (count == null) {
        refCount.put(prevRef, 1);
      } else {
        refCount.put(prevRef, count + 1);
      }
      prevInfo = prevInfo.prevTraversalInfo;
    }

    // block further traversal for declared methods that appeared in 10% or more
    // "frames" in the current call chain (it's just a heuristic)
    boolean traversalStopsAdded = false;
    for (Map.Entry<MethodReference, Integer> entry : refCount.entrySet()) {
      if (entry.getValue() > callChainLen / 10) {
        MethodReference ref = entry.getKey();
        if (!traversalStops.contains(ref)) {
          // check for method reference existence so that we can tell
          // if anything new has been added
          traversalStops.add(ref);
          traversalStopsAdded = true;
        }
      }
    }
    return traversalStopsAdded;
  }

  // method references are canonicalized and can be compared with ==
  @SuppressWarnings("ReferenceEquality")
  private TraversalInfo findMissingMethods(
      IMethod targetMethod,
      MethodReference declaredMethodRef,
      IClassLoader appLoader,
      @Nullable TraversalInfo prevTraversalInfo,
      ClassHierarchy cha,
      Map<CallSiteReference, Set<IMethod>> postTargetCache,
      Map<TypeName, String> extClassJarInfos,
      Set<TypeName> preExtClasses,
      int depth)
      throws Exception {
    MethodReference targetRef = targetMethod.getReference();
    MethodInfo targetMethodInfo =
        new MethodInfo(targetRef, getJarInfo(targetRef, extClassJarInfos));
    TraversalInfo traversalInfo = traversalTracker.get(targetMethodInfo);
    if (traversalInfo == null || allocations.size() > traversalInfo.numAllocations) {
      if (traversalInfo != null) {
        // we have already seen this method - simplify reported call chain by resuming
        // traversal from the first time we saw it - otherwise we risk reporting
        // long call chains with the same repeated sequences that are not very readable
        TraversalInfo prevInfo = prevTraversalInfo;
        while (prevInfo != null) {
          if (prevInfo.targetMethod == targetRef) {
            traversalInfo.state = TraversalState.VISITED;
            traversalInfo.numAllocations = allocations.size();
            return traversalInfo;
          }
          prevInfo = prevInfo.prevTraversalInfo;
        }
      }
      traversalInfo = new TraversalInfo(TraversalState.INITIAL, declaredMethodRef, targetRef);
      traversalTracker.put(targetMethodInfo, traversalInfo);

      // record info about allocation sites for this method
      for (NewSiteReference site : CodeScanner.getNewSites(targetMethod)) {
        TypeName className = site.getDeclaredType().getName();
        addClassIfInScope(appLoader, className, allocations);
      }

      traversalInfo.prevTraversalInfo = prevTraversalInfo;
      // update number of allocations to avoid erroneously visiting here again
      traversalInfo.numAllocations = allocations.size();

      if (depth >= analyzer.input.maxCallChainLen) {
        if (updateTraversalStops(traversalInfo, depth)) {
          // restart only if any traversal stops have been added,
          // otherwise an infinite loop could happen after restart
          // as we could be exploring exactly the same paths
          traversalInfo.state = TraversalState.RESTART;
          // reset traversal tracker so that we can explore paths we
          // have seen until now, otherwise we may miss something
          // (this will lead to some repeated work but reaching this
          // call chain length should be a rare occasion)
          traversalTracker.clear();
          return traversalInfo;
        }
      }

      int numAllocations = allocations.size();
      while (true) {
        // iterate until set of allocations does not grow anymore
        TraversalInfo foundInfo = null;
        for (CallSiteReference site : CodeScanner.getCallSites(targetMethod)) {
          MethodReference siteMethodRef = site.getDeclaredTarget();
          if (!traversalStops.contains(siteMethodRef)) {
            // during traversal visiting certain declared methods with
            // a huge number of targets should be avoided; an example
            // here are auto-generated methods from communication
            // layer packages (e.g. getScheme() from Apache Thrift)
            TypeName siteClassName = siteMethodRef.getDeclaringClass().getName();
            if (!siteClassName.equals(OBJECT_TYPE_NAME)) {
              if (preExtClasses.contains(siteClassName)
                  && isMissingScopeMethod(siteMethodRef, siteClassName, appLoader)
                  && !Analyzer.isClassExcluded(siteClassName, analyzer.input.methodExcludes)) {
                // create traversal info for the missing method reference found
                TraversalInfo foundTraversalInfo =
                    new TraversalInfo(TraversalState.FOUND, siteMethodRef, siteMethodRef);
                MethodInfo siteMethodInfo =
                    new MethodInfo(siteMethodRef, getJarInfo(siteMethodRef, extClassJarInfos));
                traversalTracker.put(siteMethodInfo, foundTraversalInfo);
                foundTraversalInfo.numAllocations = allocations.size();
                foundTraversalInfo.prevTraversalInfo = traversalInfo;
                // mark current caller's traversal info
                traversalInfo.numAllocations = allocations.size();
                traversalInfo.lineNumber = targetMethod.getLineNumber(site.getProgramCounter());
                return foundTraversalInfo;
              }
              Set<IMethod> targets = getPossibleTargets(site, cha, postTargetCache);
              for (IMethod t : targets) {
                TypeReference classRef = t.getDeclaringClass().getReference();
                if (t.isStatic() || t.isClinit() || allocations.contains(classRef)) {
                  // only consider paths to methods whose classes have already been allocated
                  foundInfo =
                      findMissingMethods(
                          t,
                          siteMethodRef,
                          appLoader,
                          traversalInfo,
                          cha,
                          postTargetCache,
                          extClassJarInfos,
                          preExtClasses,
                          depth + 1);
                  if (foundInfo.state == TraversalState.RESTART) {
                    return foundInfo;
                  } else if (foundInfo.state == TraversalState.VISITED) {
                    if (foundInfo.targetMethod != targetRef) {
                      // keep returning from recursion until you hit the same method
                      // as the one reached down the call path
                      return foundInfo;
                    } else {
                      // make sure we re-loop
                      numAllocations = 0;
                      break;
                    }
                  } else if (foundInfo.state == TraversalState.FOUND) {
                    // update data on the discovered call path
                    traversalInfo.numAllocations = allocations.size();
                    traversalInfo.lineNumber = targetMethod.getLineNumber(site.getProgramCounter());
                    return foundInfo;
                  }
                }
              }
            }
            if (foundInfo != null && foundInfo.state == TraversalState.VISITED) {
              // we found the point where we need to restart the search
              foundInfo.state = TraversalState.INITIAL;
              break;
            }
          }
        }
        if (allocations.size() == numAllocations) {
          break;
        } else {
          numAllocations = allocations.size();
        }
      }
      traversalInfo.numAllocations = allocations.size();
    }
    return traversalInfo;
  }

  private Map<IMethod, Set<CallSiteReference>> makeEntryPoints(
      Set<TypeName> rootClasses, ClassHierarchy cha, IClassLoader appLoader) throws Exception {
    Map<IMethod, Set<CallSiteReference>> entries = new HashMap<>();
    for (IClassLoader cl : cha.getLoaders()) {
      if (cl.getReference().equals(ClassLoaderReference.Application)) {
        for (IClass c : Iterator2Iterable.make(cl.iterateAllClasses())) {
          if (rootClasses.contains(c.getName())) {
            for (IMethod m : c.getDeclaredMethods()) {
              // record info about allocation sites
              for (NewSiteReference site : CodeScanner.getNewSites(m)) {
                TypeName className = site.getDeclaredType().getName();
                addClassIfInScope(appLoader, className, allocations);
              }

              Set<CallSiteReference> callSites = new HashSet<>();
              // entry points are methods that contain some external calls
              for (CallSiteReference site : CodeScanner.getCallSites(m)) {
                TypeName siteClassName = site.getDeclaredTarget().getDeclaringClass().getName();
                if (!rootClasses.contains(siteClassName)) {
                  // pkg can be null for arrays (perhaps others)
                  callSites.add(site);
                }
              }
              if (callSites.size() > 0) {
                entries.put(m, callSites);
              }
            }
          }
        }
      }
    }
    return entries;
  }

  private Set<IMethod> getPossibleTargets(
      CallSiteReference site,
      ClassHierarchy cha,
      Map<CallSiteReference, Set<IMethod>> targetCache) {
    MethodReference declaredTarget = site.getDeclaredTarget();
    if (Analyzer.isApplicationClass(declaredTarget.getDeclaringClass().getName().getPackage())) {
      // do not consider targets for methods defined in system classes;
      // not only would callgraph explode but also the number of
      // paths that need to be considered in such cases makes the
      // result practically useless
      Set<IMethod> result = targetCache.get(site);
      if (result == null) {
        if (site.isDispatch()) {
          result = cha.getPossibleTargets(declaredTarget);
        } else {
          IMethod m = cha.resolveMethod(declaredTarget);
          if (m != null) {
            result = Collections.singleton(m);
          } else {
            result = Collections.emptySet();
          }
        }

        if (!result.equals(Collections.emptySet())) {
          Set<IMethod> finalResult = new HashSet<>();
          for (IMethod m : result) {
            if (Analyzer.isApplicationClass(m.getDeclaringClass().getName().getPackage())) {
              finalResult.add(m);
            }
          }
          result = finalResult;
        }
        targetCache.put(site, result);
      }
      return result;
    } else {
      return Collections.emptySet();
    }
  }

  private enum TraversalState {
    INITIAL,
    FOUND,
    VISITED,
    RESTART
  }

  /**
   * Represents information about methods encountered during missing method discovery traversals.
   * Since the analysis works across multiple targets, a given method reference may represent more
   * then one actual method as separate targets may use the same class/method declaration but a
   * different implementation (e.g. in earlier and later version of the same library). Consequently,
   * we need to identify a method during traversal not only using its reference but also a jar it
   * originates from.
   */
  private static class MethodInfo {
    // method identified as one of the actual targets
    MethodReference targetMethod;

    // info string for the jar file containing target method
    String jarInfo;

    MethodInfo(MethodReference targetMethod, String jarInfo) {
      this.targetMethod = targetMethod;
      this.jarInfo = jarInfo;
    }

    @Override
    public int hashCode() {
      return Objects.hash(targetMethod.hashCode(), jarInfo.hashCode());
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof MethodInfo)) {
        return false;
      } else {
        MethodInfo other = ((MethodInfo) o);
        return other.targetMethod == targetMethod && other.jarInfo.equals(jarInfo);
      }
    }
  }

  private static class TraversalInfo {
    TraversalState state;

    // method statically declared at the call site
    final MethodReference declaredMethod;

    // method identified as one of the actual targets
    final MethodReference targetMethod;

    @Nullable TraversalInfo prevTraversalInfo;

    int numAllocations;

    int lineNumber;

    TraversalInfo(
        TraversalState state, MethodReference declaredMethod, MethodReference targetMethod) {
      this.state = state;
      this.declaredMethod = declaredMethod;
      this.targetMethod = targetMethod;
      this.prevTraversalInfo = null;
      this.numAllocations = 0;
      this.lineNumber = -1;
    }

    @Override
    public int hashCode() {
      return targetMethod.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof TraversalInfo)) {
        return false;
      } else {
        return ((TraversalInfo) o).targetMethod == targetMethod;
      }
    }
  }
}
