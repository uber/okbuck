package com.uber.depvalidator;

import com.google.common.base.Preconditions;
import com.ibm.wala.types.TypeName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Implements an analysis to look for classes that went missing as a result of a third-party library
 * dependency change.
 */
public class ClassAnalyzer {

  private static final int NUM_HITS = 10;

  private final Analyzer analyzer;

  /** A set of discovered paths to missing classes (to avoid reporting redundancy) */
  private final Set<Integer> classPathHashes;

  /**
   * Constructs the missing class analyzer.
   *
   * @param analyzer a reference to the main analyzer object
   */
  public ClassAnalyzer(Analyzer analyzer) {
    this.analyzer = analyzer;
    this.classPathHashes = new HashSet<>();
  }

  /**
   * Reports missing class information, if any.
   *
   * @param foundInfo result of the missing class traversal
   * @param target build target (service) where the missing class is searched (used for reporting
   *     purposes)
   * @param firstHit determines if it is the first time a path to a missing class has been
   *     discovered
   * @param rootClasses post-commit classes implementing logic of a given service whose methods are
   *     to be used as starting points for call chain discovery
   * @param extClassJarInfos a map from post-commit third-party library classes to jar file info
   *     string (hash and path) for a jar defining these classes
   * @return a value determining if an actual non-redundant path to a missing class has been
   *     reported
   */
  private boolean reportMissingClass(
      TraversalInfo foundInfo,
      String target,
      boolean firstHit,
      Set<TypeName> rootClasses,
      Map<TypeName, String> extClassJarInfos) {
    if (Analyzer.isClassExcluded(foundInfo.className, analyzer.input.classExcludes)) {
      return false;
    }
    TraversalInfo prevFoundInfo = foundInfo;
    List<TypeName> classPath = new ArrayList<>();
    classPath.add(prevFoundInfo.className);
    while (true) {
      prevFoundInfo = prevFoundInfo.prevTraversalInfo;
      if (prevFoundInfo == null) {
        break;
      } else {
        classPath.add(prevFoundInfo.className);
      }
    }
    int classPathHash =
        analyzer.getExtPathHash(
            classPath,
            (c) -> {
              return c;
            },
            rootClasses);
    if (!classPathHashes.contains(classPathHash)) {
      // report only call paths that are unique
      classPathHashes.add(classPathHash);
      if (firstHit) {
        System.out.println(
            "MISSING REACHABLE CLASSES FOUND" + (target == null ? "" : " FOR " + target) + ":");
      }
      if (analyzer.input.verbose) {
        for (int i = classPath.size() - 1; i >= 0; i--) {
          TypeName className = classPath.get(i);
          String jarInfo = extClassJarInfos.get(className);
          if (jarInfo == null) {
            // what this really means is that this class is not a
            // third-party one and we report class location only for
            // third-party jar files
            System.out.println("\t" + Analyzer.getClassNameString(className));
          } else {
            String jarFileName = Analyzer.extractJarFileName(jarInfo);
            System.out.println(
                "\t" + Analyzer.getClassNameString(className) + " [" + jarFileName + "]");
          }
        }
        System.out.println("");
      } else {
        System.out.println("\t" + Analyzer.getClassNameString(foundInfo.className));
      }
      return true;
    }
    return false;
  }

  /**
   * Determines a set of classes present in the pre-commit service fat jar but missing in the
   * post-commit service fat jar. It considers two sets of missing classes:
   *
   * <ul>
   *   <li>classes "reachable" from the service code through constant pool references (will report a
   *       "reachability path" for these)
   *   <li>classes declared in the service's code metadata (will only report missing class names)
   * </ul>
   *
   * <p>Similarly to method analyzer, it is not guaranteed that all missing classes will be reported
   * during the first analyzer run (as not all "paths" are going to be explored during this run),
   * but if any discoverable paths exist, at least one will be reported. Once initial set of missing
   * references is taken care of, the anlalyzer should be re-ran.
   *
   * @param numFound number of missing classes found so far across all targets
   * @param target name of the target (aka service) being analyzed
   * @param additionalReferencedClasses (out argument) classes transitively derived from classes
   *     listed in fat jar's metadata
   * @param additionalClassDependencies additional classes discovered in the post-commit service fat
   *     jar's metadata
   * @param rootClasses post-commit classes implementing logic of a given service whose methods are
   *     to be used as starting points for call chain discovery
   * @param missingClasses a "raw" set of third-party library classes that are present in the
   *     pre-commit service fat jar but are missing in the post-commit service fat jar
   * @param classRefs a mapping classRefs from post-commit classes to the (also post-commit) classes
   *     referenced
   * @param extClassJarInfos a map from post-commit third-party library classes to jar file info
   *     string (hash and path) for a jar defining these classes
   * @param preExtClasses pre-commit third-party library classes
   * @return <code>true</code> number of missing classes found so far across all targets, inluding
   *     missing classes found during this analyze method call
   */
  public int analyze(
      int numFound,
      String target,
      Set<TypeName> additionalReferencedClasses,
      Set<TypeName> additionalClassDependencies,
      Set<TypeName> rootClasses,
      Set<TypeName> missingClasses,
      Map<TypeName, List<TypeName>> classRefs,
      Map<TypeName, String> extClassJarInfos,
      Set<TypeName> preExtClasses)
      throws Exception {
    Preconditions.checkArgument(additionalReferencedClasses.size() == 0);

    Map<TypeName, TraversalInfo> traversalTracker = new HashMap<>();
    for (TypeName rootName : rootClasses) {
      List<TypeName> refs = classRefs.get(rootName);
      if (refs != null) {
        for (TypeName refClass : refs) {
          TraversalInfo foundInfo =
              findMissingReachableClasses(
                  traversalTracker, refClass, null, missingClasses, classRefs);
          if (foundInfo != null && foundInfo.state == TraversalState.FOUND) {
            numFound +=
                reportMissingClass(foundInfo, target, numFound == 0, rootClasses, extClassJarInfos)
                    ? 1
                    : 0;
            if (numFound >= analyzer.input.reportedProblems) {
              break;
            }
          }
        }
      }
      if (numFound >= analyzer.input.reportedProblems) {
        break;
      }
    }

    if (numFound < analyzer.input.reportedProblems) {
      // what we get from getDependencies() above are canonicalized class names
      // they themselves can be ultimately missing, but so can their own dependencies
      // that are not necessarily "reachable" in any other way; consequently
      // we need to consider all additional dependent classes as well as the transitive
      // closure of their own dependencies
      // as an example consider a reflectively loaded class that by itself is present
      // in the post-upgrade jar but whose dependency (e.g. a class representing its
      // field type) is not
      classDepClosure(
          additionalReferencedClasses, additionalClassDependencies, classRefs, preExtClasses);

      boolean firstHit = true;
      for (TypeName className : missingClasses) {
        if (Analyzer.isClassExcluded(className, analyzer.input.classExcludes)) {
          continue;
        }
        if (additionalReferencedClasses.contains(className) && preExtClasses.contains(className)) {
          if (firstHit) {
            firstHit = false;
            System.out.println(
                "MISSING RESOURCE CLASSSES FOUND" + (target == null ? "" : " FOR " + target) + ":");
          }
          System.out.println("\t" + Analyzer.getClassNameString(className));
          numFound++;
          if (numFound >= analyzer.input.reportedProblems) {
            break;
          }
        }
      }
    }
    return numFound;
  }

  private @Nullable TraversalInfo findMissingReachableClasses(
      Map<TypeName, TraversalInfo> traversalTracker,
      TypeName prevClass,
      @Nullable TraversalInfo prevTraversalInfo,
      Set<TypeName> missingClasses,
      Map<TypeName, List<TypeName>> classRefs) {
    TraversalInfo traversalInfo = traversalTracker.get(prevClass);
    if (traversalInfo == null) {
      traversalInfo = new TraversalInfo(TraversalState.INITIAL, prevClass, prevTraversalInfo);
      traversalTracker.put(prevClass, traversalInfo);
      List<TypeName> refs = classRefs.get(prevClass);
      if (refs != null) {
        for (TypeName refClass : refs) {
          if (missingClasses.contains(refClass)) {
            TraversalInfo foundTraversalInfo =
                new TraversalInfo(TraversalState.FOUND, refClass, traversalInfo);
            return foundTraversalInfo;
          } else {
            TraversalInfo foundTraversalInfo =
                findMissingReachableClasses(
                    traversalTracker, refClass, traversalInfo, missingClasses, classRefs);
            if (foundTraversalInfo != null && foundTraversalInfo.state == TraversalState.FOUND) {
              return foundTraversalInfo;
            }
          }
        }
      }
    }

    return traversalInfo;
  }

  private void addReferencedClass(
      TypeName className,
      Set<TypeName> referencedClasses,
      Map<TypeName, List<TypeName>> classRefs,
      Set<TypeName> preExtClasses)
      throws Exception {
    if (preExtClasses.contains(className)) {
      // only care about third party classes
      if (!referencedClasses.contains(className)) {
        referencedClasses.add(className);
        List<TypeName> refs = classRefs.get(className);
        if (refs != null) {
          for (TypeName refClass : refs) {
            addReferencedClass(refClass, referencedClasses, classRefs, preExtClasses);
          }
        } // else: referenced class is missing from the post set
      }
    }
  }

  private void classDepClosure(
      Set<TypeName> referencedClasses,
      Set<TypeName> additionalClassDependencies,
      Map<TypeName, List<TypeName>> classRefs,
      Set<TypeName> preExtClasses)
      throws Exception {
    for (TypeName dep : additionalClassDependencies) {
      addReferencedClass(dep, referencedClasses, classRefs, preExtClasses);
    }
  }

  private enum TraversalState {
    INITIAL,
    FOUND
  }

  private static class TraversalInfo {

    TraversalState state;

    final TypeName className;

    @Nullable TraversalInfo prevTraversalInfo;

    TraversalInfo(
        TraversalState state, TypeName className, @Nullable TraversalInfo prevTraversalInfo) {
      this.state = state;
      this.className = className;
      this.prevTraversalInfo = prevTraversalInfo;
    }
  }
}
