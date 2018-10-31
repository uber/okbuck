package com.uber.okbuck.core.manager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sun.istack.Nullable;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.Prebuilt;
import com.uber.okbuck.template.jvm.JvmBinaryRule;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.io.File;
import java.util.Map;
import java.util.Set;

public final class JetifierManager {

    private static final String JETIFIER_LOCATION =
            OkBuckGradlePlugin.WORKSPACE_PATH + "/jetifier";
    private static final String JETIFIER_BUCK_FILE = JETIFIER_LOCATION + "/BUCK";
    private static final String JETIFIER_DEPS_CONFIG = "okbuck_jetifier_deps";
    private static final String JETIFIER_CACHE_LOCATION = "com/android/tools/build/jetifier";
    private static final String JETIFIER_GROUP = JETIFIER_CACHE_LOCATION
            .replace("/", ".");
    private static final String JETIFIER_CLI_CLASS = "com.android.tools.build.jetifier.standalone.Main";
    private static final String JETIFIER_BINARY_RULE_NAME = "okbuck_jetifier";
    private static final String JETIFIER_VERSION = "1.0.0-beta02";

    private static final ImmutableList<String> JETIFIER_MODULES =
            ImmutableList.of(
                    "jetifier-core",
                    "jetifier-processor"
            );
    private static final ImmutableList<String> INTERNAL_MODULES =
            ImmutableList.of(
                    "commons-cli.jar",
                    "jetifier-standalone.jar"
            );

    @Nullable private Set<String> dependencies;
    private final Project project;

    public JetifierManager(Project project) {
        this.project = project;
    }

    public static boolean isJetifierEnabled(Project project) {
        Map<String, ?> properties = project.getProperties();

        if (properties.containsKey("android.useAndroidX")
                && properties.containsKey("android.enableJetifier")) {
            return (Boolean.valueOf((String) properties.get("android.useAndroidX")))
                    && (Boolean.valueOf((String) properties.get("android.enableJetifier")));
        }
        return false;
    }

    public void setupJetifier() {
        Configuration jetifierConfig = project.getConfigurations().maybeCreate(JETIFIER_DEPS_CONFIG);
        DependencyHandler handler = project.getDependencies();
        JETIFIER_MODULES
                .stream()
                .map(module -> String.format("%s:%s:%s", JETIFIER_GROUP, module, JETIFIER_VERSION))
                .forEach(dependency -> handler.add(JETIFIER_DEPS_CONFIG, dependency));

        dependencies = new DependencyCache(project, ProjectUtil.getDependencyManager(project)).build(jetifierConfig);
    }

    public void finalizeDependencies() {
        if (dependencies != null && dependencies.size() > 0) {
            ImmutableList.Builder<Rule> rulesBuilder = new ImmutableList.Builder<>();
            ImmutableSet.Builder<String> binaryDependencies = ImmutableSet.builder();
            binaryDependencies.addAll(BuckRuleComposer.external(dependencies));

            for (String module : INTERNAL_MODULES) {
                FileUtil.copyResourceToProject(
                        "jetifier/" + module, new File(JETIFIER_LOCATION, module));
                rulesBuilder.add(
                        new Prebuilt()
                                .prebuiltType(RuleType.PREBUILT_JAR.getProperties().get(0))
                                .prebuilt(module)
                                .ruleType(RuleType.PREBUILT_JAR.getBuckName())
                                .name(module));
                binaryDependencies.add(":" + module);
            }

            rulesBuilder.add(
                    new JvmBinaryRule()
                            .mainClassName(JETIFIER_CLI_CLASS)
                            .deps(binaryDependencies.build())
                            .ruleType(RuleType.JAVA_BINARY.getBuckName())
                            .name(JETIFIER_BINARY_RULE_NAME)
                            .defaultVisibility());

            File buckFile = project.getRootProject().file(JETIFIER_BUCK_FILE);
            FileUtil.writeToBuckFile(rulesBuilder.build(), buckFile);
        }
    }
}
