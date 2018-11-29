package com.uber.okbuck.composer.jvm;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JvmBuckRuleComposer extends BuckRuleComposer {

  private static final ImmutableMap<Scope.Builder.COMPILER, String> COMPILER_OPTIONS_MAP =
      ImmutableMap.of(
          Scope.Builder.COMPILER.JAVA, "extra_arguments",
          Scope.Builder.COMPILER.KOTLIN, "extra_kotlinc_arguments",
          Scope.Builder.COMPILER.SCALA, "extra_arguments");

  public static String src(JvmTarget target) {
    return "src_" + target.getName();
  }

  public static String bin(JvmTarget target) {
    return "bin_" + target.getName();
  }

  public static String test(JvmTarget target) {
    return "test_" + target.getName();
  }

  /**
   * Get api and implementation target deps. deps = runtimeClasspath(api + implementation +
   * runtimeOnly) intersect compileClasspath(api + implementation + compileOnly)
   *
   * @param runtime RuntimeClasspath scope
   * @param compile CompileClasspath scope
   * @return Target deps
   */
  public static Set<Target> getTargetDeps(Scope runtime, Scope compile) {
    return Sets.intersection(runtime.getTargetDeps(), compile.getTargetDeps());
  }

  /**
   * Get compileOnly target deps. compileOnlyDeps = compileClasspath(api + implementation +
   * compileOnly) - runtimeClasspath(api + implementation + runtimeOnly)
   *
   * @param runtime RuntimeClasspath scope
   * @param compile CompileClasspath scope
   * @return CompileOnly Target deps
   */
  public static Set<Target> getTargetProvidedDeps(Scope runtime, Scope compile) {
    return Sets.difference(compile.getTargetDeps(), runtime.getTargetDeps());
  }

  /**
   * Get api and implementation external deps. deps = runtimeClasspath(api + implementation +
   * runtimeOnly) intersect compileClasspath(api + implementation + compileOnly)
   *
   * @param runtime RuntimeClasspath scope
   * @param compile CompileClasspath scope
   * @return External deps
   */
  public static Set<ExternalDependency> getExternalDeps(Scope runtime, Scope compile) {
    return Sets.intersection(runtime.getExternalDeps(), compile.getExternalDeps());
  }

  /**
   * Get compileOnly external deps. compileOnlyDeps = compileClasspath(api + implementation +
   * compileOnly) - runtimeClasspath(api + implementation + runtimeOnly)
   *
   * @param runtime RuntimeClasspath scope
   * @param compile CompileClasspath scope
   * @return CompileOnly Target deps
   */
  public static Set<ExternalDependency> getExternalProvidedDeps(Scope runtime, Scope compile) {
    return Sets.difference(compile.getExternalDeps(), runtime.getExternalDeps());
  }

  /**
   * Returns the ap's plugin rules path
   *
   * @param aps Annotation Processor plugin's UID
   * @return Set of java annotation processor plugin's rule paths.
   */
  public static Set<String> getApPlugins(Set<String> aps) {
    return aps.stream().map(JvmBuckRuleComposer::getApPluginRulePath).collect(Collectors.toSet());
  }

  /**
   * Returns the java annotation processor plugin's rule name using the pluginUID
   *
   * @param pluginUID pluginUID used to get the rule name
   * @return Plugin rule name.
   */
  protected static String getApPluginRuleName(String pluginUID) {
    return String.format("processor_%s", pluginUID);
  }

  /**
   * Returns the buck specific compiler options for various jvm compilers
   *
   * @param options compiler options map
   * @return The compiler options
   */
  protected static Map<String, List<String>> mapOptions(
      Map<Scope.Builder.COMPILER, List<String>> options) {
    return options
        .entrySet()
        .stream()
        .collect(Collectors.toMap(e -> COMPILER_OPTIONS_MAP.get(e.getKey()), Map.Entry::getValue));
  }

  /**
   * Returns the java annotation processor plugin's rule path using the pluginUID
   *
   * @param pluginUID pluginUID used to get the rule path
   * @return Plugin rule path.
   */
  private static String getApPluginRulePath(String pluginUID) {
    return String.format(
        "//" + OkBuckGradlePlugin.WORKSPACE_PATH + "/processor:%s", getApPluginRuleName(pluginUID));
  }
}
