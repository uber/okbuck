package com.uber.depvalidator;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.strings.Atom;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * This is the main class of a dependency validation tool. It is meant to discover potential
 * dependency violations during third-party library version upgrades.
 */
public class Analyzer {

  static final String EXT_DIR = "3rdparty";

  // max default length of call chain that should be treversed to seed a missing method
  // (if it's too long, it may lead to stack overflow error during recursion)
  static final int MAX_CHAIN_LEN_DEFAULT = 1000;

  // number of missing classes (and missing methods) reported by
  // default across all targets
  static final int REPORTED_PROBLEMS_DEFAULT = 3;

  static final long TIMEOUT_DEFAULT_MS = 90 * 1000;

  private static final Atom JAVA_PKG_ATOM = Atom.findOrCreateAsciiAtom("java/");

  private static final Atom JAVAX_PKG_ATOM = Atom.findOrCreateAsciiAtom("javax/");

  private static final Atom SCALA_PKG_ATOM = Atom.findOrCreateAsciiAtom("scala/");

  private static final Atom COM_SUN_PKG_ATOM = Atom.findOrCreateAsciiAtom("com/sun/");

  private static final Atom SUN_PKG_ATOM = Atom.findOrCreateAsciiAtom("sun/");

  private static final String METHOD_EXCLUDES_OPT = "method_excludes";

  private static final String CLASS_EXCLUDES_OPT = "class_excludes";

  private static final String ALLMISSING_OPT = "allmissing";

  private static final String VERBOSE_OPT = "verbose";

  private static final String PRIV_CLASSES_OPT = "priv_classes";

  private static final String DIFFERENT_PATHS_OPT = "different_paths";

  private static final String MAX_CHAIN_LEN_OPT = "max_chain_len";

  private static final String REPORTED_PROBLEMS_OPT = "reported_problems";

  private static final String TIMEOUT_OPT = "timeout";

  private static final String PRE_EXT_JARS_OPT = "pre_ext_jars";

  private static final String PRE_ALL_JARS_OPT = "pre_all_jars";

  private static final String EXT_JARS_OPT = "ext_jars";

  private static final String ALL_JARS_OPT = "all_jars";

  private static final String ROOT_JARS_OPT = "root_jars";

  private static final Function<String, String> getJarPathFromLine =
      (s) -> {
        // only consider lines that end with ".jar"
        int lastDotIndex = s.lastIndexOf(".");
        if (lastDotIndex != -1 && s.substring(lastDotIndex).equals(".jar")) {
          return s;
        } else {
          return null;
        }
      };

  final AnalyzerInput input;

  private final AdditionalClassDependencyReader additionalDependencyReader;

  private final ClassAnalyzer classAnalyzer;

  private final MethodAnalyzer methodAnalyzer;

  public Analyzer(AnalyzerInput input) throws Exception {
    this.input = input;
    this.additionalDependencyReader = new AdditionalClassDependencyReader();
    this.classAnalyzer = new ClassAnalyzer(this);
    this.methodAnalyzer = new MethodAnalyzer(this);
  }

  @Nullable
  private static CommandLine parseArgs(String[] args) {
    Options options = new Options();

    // the config file must contain the following heather and input values
    // as comma-separated values conforming to the header field order:
    //
    // "pre_ext_jars","pre_all_jars","ext_jars","all_jars","root_jars","fat_jar"
    Option configFile =
        new Option("config_file", true, "file containing input paths in the csv format");
    configFile.setRequired(true);
    options.addOption(configFile);

    // the following two options allow altering the tool output by
    // excluding individual class sets (specified by class name
    // prefixes) from being included; having one option for missing
    // class search and the other for missing method search (where all
    // methods that belong to specified class set are excluded) allows
    // for finer degree of control as the class sets meant for
    // exclusion may have no overlap between these two types of search
    Option methodExcludesFile =
        new Option(
            METHOD_EXCLUDES_OPT,
            true,
            "(optional) text file containing list of class prefixes NOT considered for \"missing\" methods lookup");
    options.addOption(methodExcludesFile);

    Option classExcludesFile =
        new Option(
            CLASS_EXCLUDES_OPT,
            true,
            "(optional) text file containing list of class prefixes NOT considered for \"missing\" classes lookup");
    options.addOption(classExcludesFile);

    Option allMissing =
        new Option(
            ALLMISSING_OPT,
            false,
            "provide paths to missing methods even if they are also present in the pre-update jar");
    options.addOption(allMissing);

    Option classPaths =
        new Option(VERBOSE_OPT, false, "print \"paths\" from service code to missing classes");
    options.addOption(classPaths);

    Option privClasses =
        new Option(
            PRIV_CLASSES_OPT,
            false,
            "include private and package-private classes in the set of missing ones");
    options.addOption(privClasses);

    Option differentPaths =
        new Option(
            DIFFERENT_PATHS_OPT,
            false,
            "report a missing method or classes more than once if they can be reached via different paths");
    options.addOption(differentPaths);

    Option maxChainLen =
        new Option(MAX_CHAIN_LEN_OPT, true, "avoid exploring call chains longer than max");
    maxChainLen.setType(Number.class);
    options.addOption(maxChainLen);

    Option reportedProblems =
        new Option(REPORTED_PROBLEMS_OPT, true, "number of reported problems (per problem class)");
    reportedProblems.setType(Number.class);
    options.addOption(reportedProblems);

    Option timeoutMs = new Option(TIMEOUT_OPT, true, "per-target analysis timeout (in seconds)");
    timeoutMs.setType(Number.class);
    options.addOption(timeoutMs);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();

    try {
      CommandLine cmd = parser.parse(options, args);
      // pre-parse integer options here - this is arguably the cleanest
      // way to consistently print the help message in cases when
      // command-line options are incorrect
      if (cmd.hasOption(MAX_CHAIN_LEN_OPT)) {
        cmd.getParsedOptionValue(MAX_CHAIN_LEN_OPT);
        int maxCallChainLenVal = ((Number) cmd.getParsedOptionValue(MAX_CHAIN_LEN_OPT)).intValue();
        if (maxCallChainLenVal <= 0) {
          throw new ParseException("max call chain length to discover should be greater than one");
        }
      }
      if (cmd.hasOption(REPORTED_PROBLEMS_OPT)) {
        cmd.getParsedOptionValue(REPORTED_PROBLEMS_OPT);
        int reportedProblemsVal =
            ((Number) cmd.getParsedOptionValue(REPORTED_PROBLEMS_OPT)).intValue();
        if (reportedProblemsVal <= 0) {
          throw new ParseException("number of problem to report should be greater than one");
        }
      }
      if (cmd.hasOption(TIMEOUT_OPT)) {
        long timeoutMsVal = ((Number) cmd.getParsedOptionValue(TIMEOUT_OPT)).longValue();
        if (timeoutMsVal < 0) {
          throw new ParseException("timout value should not be negative");
        }
      }
      return cmd;
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("Analyzer", options);
      return null;
    }
  }

  @Nullable
  private static AnalyzerInput getInput(String[] args) throws Exception {
    CommandLine cmd = parseArgs(args);
    if (cmd == null) {
      return null;
    }

    int maxCallChainLen = MAX_CHAIN_LEN_DEFAULT;
    if (cmd.hasOption(MAX_CHAIN_LEN_OPT)) {
      maxCallChainLen = ((Number) cmd.getParsedOptionValue(MAX_CHAIN_LEN_OPT)).intValue();
    }

    int reportedProblems = REPORTED_PROBLEMS_DEFAULT;
    if (cmd.hasOption(REPORTED_PROBLEMS_OPT)) {
      reportedProblems = ((Number) cmd.getParsedOptionValue(REPORTED_PROBLEMS_OPT)).intValue();
    }

    long timeoutMs = TIMEOUT_DEFAULT_MS;
    if (cmd.hasOption(TIMEOUT_OPT)) {
      timeoutMs = ((Number) cmd.getParsedOptionValue(TIMEOUT_OPT)).longValue();
    }

    AnalyzerInput input =
        new AnalyzerInput(
            getExcludes(cmd.getOptionValue(METHOD_EXCLUDES_OPT)),
            getExcludes(cmd.getOptionValue(CLASS_EXCLUDES_OPT)),
            cmd.hasOption(ALLMISSING_OPT),
            cmd.hasOption(VERBOSE_OPT),
            cmd.hasOption(PRIV_CLASSES_OPT),
            cmd.hasOption(DIFFERENT_PATHS_OPT),
            maxCallChainLen,
            reportedProblems,
            timeoutMs);

    CSVParser csvParser =
        CSVFormat.EXCEL
            .withFirstRecordAsHeader()
            .parse(new FileReader(cmd.getOptionValue("config_file")));
    for (CSVRecord record : csvParser) {
      input.addTargetInputs(
          record.get("target"),
          record.get(PRE_EXT_JARS_OPT),
          record.get(PRE_ALL_JARS_OPT),
          record.get(EXT_JARS_OPT),
          record.get(ALL_JARS_OPT),
          record.get(ROOT_JARS_OPT));
    }

    return input;
  }

  /**
   * Main entry point to the analyzer searching for missing third-party methods and classes
   * indirectly reachable from user code after committing a diff involving a third-party library
   * dependency change (directly reachable missing methods and classes can be detected at build
   * time).
   */
  public static void main(String[] args) throws Exception {

    AnalyzerInput input = getInput(args);
    if (input == null) {
      System.exit(-1);
    }

    // NullAway does not see System.exit(-1) as preventing cmd being null
    Preconditions.checkNotNull(input);

    Analyzer analyzer = new Analyzer(input);
    System.exit(analyzer.analyze() > 0 ? 0 : 1);
  }

  /*
   * Computes the class name string without the preceding "L".
   */
  static String getClassNameString(TypeName className) {
    StringBuilder sb = new StringBuilder();
    return sb.append(className.getPackage())
        .append("/")
        .append(className.getClassName())
        .toString();
  }

  /*
   * Verifies if a class is on a given list of excluded classes.
   */
  static boolean isClassExcluded(TypeName className, Set<String> excludes) {
    String classString = className.toString();
    for (String prefix : excludes) {
      if (classString.startsWith("L" + prefix)) {
        return true;
      }
    }
    return false;
  }

  /*
   * Returns a hash for the portion of the "path" that belongs to the
   * third-party library. The path may contain objects of any type
   * that can be converted to a type name representing a class.
   */
  <T> int getExtPathHash(
      List<T> path, Function<T, TypeName> classGetter, Set<TypeName> rootClasses) {
    Preconditions.checkState(path.size() > 0);
    if (!input.differentPaths) {
      // hash only on the last element of the discovered path (the
      // acutual missing class/method) to avoid reporting different
      // chains for the same missing classes/methods
      T first = path.get(0);
      // first class on the list (last in the chain) should be third-party
      Preconditions.checkState(!rootClasses.contains(classGetter.apply(first)));
      return first.hashCode();
    } else {
      for (int i = 0; i < path.size() - 1; i++) {
        if (rootClasses.contains(classGetter.apply(path.get(i)))) {
          // first class on the "path" from missing class to the user
          // classes that is not third-party (include only "path"
          // segments that belong to third-party code)
          return Arrays.hashCode(path.subList(0, i).toArray());
        }
      }
      return Arrays.hashCode(path.toArray());
    }
  }

  /*
   * Extracts jar file name from a string reprsenting (a potentially
   * unknown) jar info.
   *
   * @param jarInfo whitespace-separated string containing jar file or
   * a string representing unknown info
   * @return jar file name or a string representing unknown jar
   */
  static String extractJarFileName(String jarInfo) {
    if (!jarInfo.startsWith("unknown ")) {
      String[] dataArray = getJarInfoArray(jarInfo);
      String filePath = dataArray[1];
      Path fileName = Paths.get(filePath).getFileName();
      if (fileName != null) {
        return fileName.toString();
      }
    }
    return "unknown";
  }

  /*
   * Retrieves file path data (the path itself and content hash).
   *
   * @param jarInfo a string representing a config file line,
   * containing whitespace-separated jar file content hash and path
   * @return array containing file path in the first position and file
   * content hash in the second position
   */
  static String[] getJarInfoArray(String filePathData) {
    // assert correct data format
    String[] dataArray = filePathData.split("\\s+");
    Preconditions.checkNotNull(dataArray);
    Preconditions.checkState(dataArray.length == 2);
    return dataArray;
  }

  private static ClassHierarchy createClassHierarchy(Collection<String> jarFilePaths)
      throws Exception {
    AnalysisScope scope = AnalysisScopeReader.makePrimordialScope(null);
    for (String jarPath : jarFilePaths) {
      JarClassReader jarReader = JarClassReader.getReader(jarPath);
      scope.addToScope(ClassLoaderReference.Application, new JarDataModule(jarReader));
    }
    return ClassHierarchyFactory.makeWithRoot(scope);
  }

  /**
   * Filters a list of paths to retain only those present in the pathFilter
   *
   * @param paths (ordered) list of paths to be filtered
   * @param pathFilter set of paths to keep in the input set
   * @return filtered paths list
   */
  private static List<String> filterPaths(List<String> paths, Set<String> pathFilter) {
    ArrayList<String> result = new ArrayList<>();
    for (String s : pathFilter) {
      if (paths.contains(s)) {
        result.add(s);
      }
    }
    return result;
  }

  public int analyze() {
    int classesFound = 0;
    int methodsFound = 0;
    for (String[] targetInfo : input.targetInputs) {

      String target = targetInfo[0];
      if (target == null) {
        target = "unnamed target";
      }
      String preExtJars = targetInfo[1];
      String preAllJars = targetInfo[2];
      String extJars = targetInfo[3];
      String allJars = targetInfo[4];
      String rootJars = targetInfo[5];

      long analysisStartTime = System.currentTimeMillis();
      try {
        Set<String> preExtJarsInfo = getJarPathsSetFromFile(preExtJars);
        List<String> preAllJarsInfo = getJarPathsListFromFile(preAllJars);
        Set<String> extJarsInfo = getJarPathsSetFromFile(extJars);
        List<String> allJarsInfo = getJarPathsListFromFile(allJars);
        List<String> rootJarsInfo = getJarPathsListFromFile(rootJars);

        HashSet<TypeName> preExtClasses = new HashSet<>();
        HashSet<TypeName> missingClasses = new HashSet<>();

        // we can create class hierarchies representing fat jars out of
        // ordered jar file lists even if the list contains multiple
        // version of a given library (with some classes sharing the same
        // name but not necessarily content)
        //
        // when creating a fat jar, the first class with a given name on a
        // list of jars goes to the fat jar, and the remaining ones are
        // ignored; on the other hand, if subseqent jars representing the
        // same library contain additinoal classes, they should be
        // included
        //
        // WALA does the same thing when instantiating class loaders that
        // are then used to retrive classes; this is (sanitized) code
        // initializing a class loader in the ClassLoaderImpl class
        // (archive variable in this case represents an ordered list of
        // jar files):
        //
        // Set<ModuleEntry> classModuleEntries = HashSetFactory.make();
        // for (Module archive : modules) {
        //   Set<ModuleEntry> classFiles = getClassFiles(archive);
        //   removeClassFiles(classFiles, classModuleEntries);
        //   loadAllClasses(classFiles);
        // }
        //
        // as we can see, the classes encountered so far are removed from
        // the list and not processed
        ClassHierarchy preCha = createClassHierarchy(preAllJarsInfo);
        ClassHierarchy cha = createClassHierarchy(allJarsInfo);

        // third-party packages list can contain more jar files than actually
        // used by the service - use the list of all service's jar files
        // as a source of truth
        List<String> filteredPreExtJarsInfo = filterPaths(preAllJarsInfo, preExtJarsInfo);
        List<String> filteredExtJarsInfo = filterPaths(allJarsInfo, extJarsInfo);

        computeMissingClasses(cha, preCha, filteredPreExtJarsInfo, preExtClasses, missingClasses);

        Set<TypeName> additionalClassDependencies =
            additionalDependencyReader.getDependencies(allJarsInfo);

        PreAnalyzer preAnalyzer = new PreAnalyzer(rootJarsInfo, filteredExtJarsInfo, allJarsInfo);
        Set<TypeName> rootClasses = preAnalyzer.getRootClasses();
        Map<TypeName, String> extClassJarInfos = preAnalyzer.getExtClassJarInfos();
        Map<TypeName, List<TypeName>> classRefs = preAnalyzer.getClassRefs();

        Set<TypeName> additionalReferencedClasses = new HashSet<>();
        classesFound =
            classAnalyzer.analyze(
                classesFound,
                target,
                additionalReferencedClasses,
                additionalClassDependencies,
                rootClasses,
                missingClasses,
                classRefs,
                extClassJarInfos,
                preExtClasses);
        if (classesFound >= input.reportedProblems) {
          break;
        }
        if (System.currentTimeMillis() - analysisStartTime > input.timeoutMs) {
          // so much time spent in class analysis that there is no
          // time to even start method analysis
          break;
        }
        methodsFound =
            methodAnalyzer.analyze(
                methodsFound,
                target,
                additionalReferencedClasses,
                cha,
                !input.allMissing ? preCha : null,
                rootClasses,
                extClassJarInfos,
                preExtClasses,
                analysisStartTime);
        if (methodsFound >= input.reportedProblems) {
          break;
        }
        if (System.currentTimeMillis() - analysisStartTime > input.timeoutMs) {
          break;
        }
      } catch (Exception x) {
        System.err.println(
            "unexpected exception when analyzing "
                + target
                + "(ignoring continue with the analysis)\n"
                + x);
      } finally {
        long analysisTime = (System.currentTimeMillis() - analysisStartTime) / 1000;
        if (analysisTime > input.timeoutMs) {
          System.err.println(
              "Analysis of binary " + target + " timed out in " + analysisTime + "s");
        } else {
          System.err.println("Analyzed binary " + target + " in " + analysisTime + "s");
        }
      }
    }
    return classesFound + methodsFound;
  }

  static boolean isApplicationClass(Atom pkgName) {
    return !(pkgName == null
        || pkgName.startsWith(JAVA_PKG_ATOM)
        || pkgName.startsWith(JAVAX_PKG_ATOM)
        || pkgName.startsWith(SCALA_PKG_ATOM)
        || pkgName.startsWith(COM_SUN_PKG_ATOM)
        || pkgName.startsWith(SUN_PKG_ATOM));
  }

  /**
   * Computes a "raw" set of missing third-party library classes between pre-commit and post-commit
   * jar files by doing a simple diff. Additionally, computes a set of pre-commit third-party
   * library classes.
   *
   * @param cha class hierarchy for all post-commit service's jar files
   * @param preCha class hierarchy for all pre-commit service's jar files
   * @param preExtJarsInfo whitespace-separated tuples describing pre-commit third-party library
   *     jars: content hash and path
   * @param preExtClasses pre-commit third-party library classes (out argument)
   * @param missingClasses a "raw" set of third-party library classes that are present in the
   *     pre-commit service fat jar but are missing in the post-commit service fat jar
   */
  private void computeMissingClasses(
      ClassHierarchy cha,
      ClassHierarchy preCha,
      List<String> preExtJarsInfo,
      HashSet<TypeName> preExtClasses,
      HashSet<TypeName> missingClasses)
      throws Exception {

    HashSet<TypeReference> classesContainsSet = new HashSet<>();

    ClassHierarchy chaPreExt = createClassHierarchy(preExtJarsInfo);
    for (IClassLoader cl : chaPreExt.getLoaders()) {
      if (cl.getReference().equals(ClassLoaderReference.Application)) {
        for (IClass c : Iterator2Iterable.make(cl.iterateAllClasses())) {
          preExtClasses.add(c.getReference().getName());
        }
      }
    }

    // find missing methods and classes by comparing all classes pre
    // and post upgrade
    for (IClassLoader cl : cha.getLoaders()) {
      if (cl.getReference().equals(ClassLoaderReference.Application)) {
        for (IClass c : Iterator2Iterable.make(cl.iterateAllClasses())) {
          classesContainsSet.add(c.getReference());
        }
      }
    }

    HashSet<TypeName> preClasses = new HashSet<>();
    for (IClassLoader cl : preCha.getLoaders()) {
      if (cl.getReference().equals(ClassLoaderReference.Application)) {
        for (IClass c : Iterator2Iterable.make(cl.iterateAllClasses())) {
          TypeReference classRef = c.getReference();
          if (input.privClasses || c.isPublic()) {
            if (!classesContainsSet.contains(classRef)
                && preExtClasses.contains(classRef.getName())) {
              // add classes that are missing in the pre-upgrade set but only those that were
              // in third-party (external) packages
              missingClasses.add(classRef.getName());
            }
          }
          preClasses.add(classRef.getName());
        }
      }
    }
    preExtClasses.retainAll(preClasses);
  }

  static <T> ImmutableCollection<T> getLinesFromFile(
      Reader reader, ImmutableCollection.Builder builder, Function<String, T> lineFn)
      throws IOException {
    BufferedReader lineReader = new BufferedReader(reader);
    try {
      String line = lineReader.readLine();
      while (line != null) {
        int commentInd = line.indexOf("#");
        if (commentInd != -1) {
          // ignore all comment characters after #
          line = line.substring(0, commentInd);
        }
        line = line.trim();
        if (!line.isEmpty()) {
          T s = lineFn.apply(line);
          if (s != null) {
            builder.add(s);
          }
        }
        line = lineReader.readLine();
      }
    } finally {
      lineReader.close();
    }
    return builder.build();
  }

  private static List<String> getJarPathsListFromFile(String fileName) throws IOException {
    ImmutableList.Builder<String> jarFiles = new ImmutableList.Builder<>();
    return (List) getLinesFromFile(new FileReader(fileName), jarFiles, getJarPathFromLine);
  }

  private static Set<String> getJarPathsSetFromFile(String fileName) throws IOException {
    ImmutableSet.Builder<String> jarFiles = new ImmutableSet.Builder<>();
    return (Set) getLinesFromFile(new FileReader(fileName), jarFiles, getJarPathFromLine);
  }

  private static Set<String> getExcludes(String excludesFile) throws Exception {
    if (excludesFile != null) {
      ImmutableSet.Builder<String> excludes = new ImmutableSet.Builder<>();
      return (Set)
          Analyzer.getLinesFromFile(
              new FileReader(excludesFile),
              excludes,
              (s) -> {
                return s;
              });
    } else {
      return Collections.emptySet();
    }
  }

  interface JarPathReader extends Function<String, String> {
    @Nullable
    default String getPath(String s) {
      // only consider lines that end with ".jar"
      int lastDotIndex = s.lastIndexOf(".");
      if (lastDotIndex != -1 && s.substring(lastDotIndex).equals(".jar")) {
        return s;
      } else {
        return null;
      }
    }
  }

  static class AnalyzerInput {

    final Set<String> methodExcludes;

    final Set<String> classExcludes;

    final boolean allMissing;

    final boolean verbose;

    final boolean privClasses;

    final boolean differentPaths;

    final int maxCallChainLen;

    final int reportedProblems;

    final long timeoutMs;

    final List<String[]> targetInputs;

    AnalyzerInput(
        Set<String> methodExcludes,
        Set<String> classExcludes,
        boolean allMissing,
        boolean verbose,
        boolean privClasses,
        boolean differentPaths,
        int maxCallChainLen,
        int reportedProblems,
        long timeoutMs) {
      this.methodExcludes = methodExcludes;
      this.classExcludes = classExcludes;
      this.allMissing = allMissing;
      this.verbose = verbose;
      this.privClasses = privClasses;
      this.differentPaths = differentPaths;
      this.maxCallChainLen = maxCallChainLen;
      this.reportedProblems = reportedProblems;
      this.timeoutMs = timeoutMs;
      this.targetInputs = new ArrayList<>();
    }

    void addTargetInputs(
        @Nullable String target,
        String preExtJars,
        String preAllJars,
        String extJars,
        String allJars,
        String rootJars) {
      String[] targetInfo = new String[7];
      targetInfo[0] = target;
      targetInfo[1] = preExtJars;
      targetInfo[2] = preAllJars;
      targetInfo[3] = extJars;
      targetInfo[4] = allJars;
      targetInfo[5] = rootJars;
      targetInputs.add(targetInfo);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("METHOD EXCLUDES:\n");
      for (String e : methodExcludes) {
        sb.append(e).append("\n");
      }
      sb.append("CLASS EXCLUDES:\n");
      for (String e : classExcludes) {
        sb.append(e).append("\n");
      }
      sb.append("ALL MISSING ").append(allMissing).append("\n");
      sb.append("MAX CALL CHAIN ").append(maxCallChainLen).append("\n");
      sb.append("TARGET INPUTS:\n");
      for (String[] targetInfo : targetInputs) {
        for (String p : targetInfo) {
          sb.append(p).append(" ");
        }
        sb.append("\n");
      }
      return sb.toString();
    }
  }
}
