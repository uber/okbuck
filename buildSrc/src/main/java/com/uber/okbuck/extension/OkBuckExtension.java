package com.uber.okbuck.extension;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;

@SuppressWarnings("unused")
public class OkBuckExtension {

  /** Build Tools Version */
  @Input public String buildToolVersion = "24.0.2";

  /** Android target sdk version */
  @Input public String target = "android-24";

  /** Annotation processor classes of project dependencies */
  @Input public Map<String, List<String>> annotationProcessors = new HashMap<>();

  /** LinearAllocHardLimit used for multi-dex support. */
  @Input public Map<String, Integer> linearAllocHardLimit = new HashMap<>();

  /** Primary dex class patterns. */
  @Input public Map<String, List<String>> primaryDexPatterns = new HashMap<>();

  /** Whether to enable exopackage. */
  @Input public Map<String, Boolean> exopackage = new HashMap<>();

  /** Exopackage lib dependencies. */
  @Input public Map<String, List<String>> appLibDependencies = new HashMap<>();

  /** Proguard mapping file applied via apply mapping */
  @Input public Map<String, File> proguardMappingFile = new HashMap<>();

  /** List of build types/variant names for which to exclude generating lint rules */
  @Input public Map<String, List<String>> lintExclude = new HashMap<>();

  /** List of build types/variant names for which to exclude generating test rules */
  @Input public Map<String, List<String>> testExclude = new HashMap<>();

  /** Set of projects to generate buck configs for. Default is all subprojects of root project. */
  @SuppressWarnings("CanBeFinal")
  @Input
  public Set<Project> buckProjects;

  /** Extra buck options */
  @Input public Map<String, Map<String, Collection<String>>> extraBuckOpts = new HashMap<>();

  /** Extra buck defs */
  @Input public Set<File> extraDefs = new HashSet<>();

  /** Whether to turn on buck's resource_union to reflect gradle's resource merging behavior */
  @Input public boolean resourceUnion = true;

  /** Whether to generate android_build_config rules for library projects */
  @Input public boolean libraryBuildConfig = true;

  /** List of exclude patterns for resources to be processed by aapt */
  @Input public Set<String> excludeResources = new HashSet<>();

  /**
   * Additional dependency caches. Every value "entry" will create a new configuration
   * "entryExtraDepCache" that can be used to fetch and cache dependencies.
   */
  @Input public Set<String> extraDepCaches = new HashSet<>();

  /** Forces okbuck to fail if the project is using dynamic or snapshot dependencies */
  @Input public boolean failOnChangingDependencies = false;

  /** The prebuilt buck binary to use */
  @Input
  public String buckBinary =
      "com.github.facebook:buck:a8454d5b2b4e9ede626202d1bc5839443430e724@pex";

  private WrapperExtension wrapperExtension = new WrapperExtension();
  private KotlinExtension kotlinExtension;
  private ScalaExtension scalaExtension = new ScalaExtension();
  private IntellijExtension intellijExtension = new IntellijExtension();
  private ExperimentalExtension experimentalExtension = new ExperimentalExtension();
  private TestExtension testExtension = new TestExtension();
  private TransformExtension transformExtension = new TransformExtension();
  private LintExtension lintExtension;
  private ExternalExtension externalExtension = new ExternalExtension();

  public OkBuckExtension(Project project) {
    buckProjects = project.getSubprojects();
    kotlinExtension = new KotlinExtension(project);
    lintExtension = new LintExtension(project);
  }

  public void wrapper(Action<WrapperExtension> container) {
    container.execute(wrapperExtension);
  }

  public WrapperExtension getWrapperExtension() {
    return wrapperExtension;
  }

  public void kotlin(Action<KotlinExtension> container) {
    container.execute(kotlinExtension);
  }

  public KotlinExtension getKotlinExtension() {
    return kotlinExtension;
  }

  public void scala(Action<ScalaExtension> container) {
    container.execute(scalaExtension);
  }

  public ScalaExtension getScalaExtension() {
    return scalaExtension;
  }

  public void intellij(Action<IntellijExtension> container) {
    container.execute(intellijExtension);
  }

  public IntellijExtension getIntellijExtension() {
    return intellijExtension;
  }

  public void experimental(Action<ExperimentalExtension> container) {
    container.execute(experimentalExtension);
  }

  public ExperimentalExtension getExperimentalExtension() {
    return experimentalExtension;
  }

  public void test(Action<TestExtension> container) {
    container.execute(testExtension);
  }

  public TestExtension getTestExtension() {
    return testExtension;
  }

  public void lint(Action<LintExtension> container) {
    container.execute(lintExtension);
  }

  public LintExtension getLintExtension() {
    return lintExtension;
  }

  public void transform(Action<TransformExtension> container) {
    container.execute(transformExtension);
  }

  public TransformExtension getTransformExtension() {
    return transformExtension;
  }

  public void external(Action<ExternalExtension> container) {
    container.execute(externalExtension);
  }

  public ExternalExtension getExternalExtension() {
    return externalExtension;
  }
}
