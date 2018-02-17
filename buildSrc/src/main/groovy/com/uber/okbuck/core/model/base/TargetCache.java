package com.uber.okbuck.core.model.base;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.BaseVariant;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.android.AndroidLibTarget;
import com.uber.okbuck.core.model.groovy.GroovyLibTarget;
import com.uber.okbuck.core.model.java.JavaLibTarget;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.core.model.kotlin.KotlinLibTarget;
import com.uber.okbuck.core.model.scala.ScalaLibTarget;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.core.util.FileUtil;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePluginConvention;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.Map;

import static com.uber.okbuck.core.util.LintUtil.LINT_DEPS_CACHE;

public class TargetCache {

    private static final Converter<String, String> NAME_CONVERTER =
            CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN);

    private final Map<Project, Map<String, Target>> store = new HashMap<>();
    private final Map<Project, Map<String, Target>> artifactNameToTarget = new HashMap<>();
    private final Map<String, String> lintConfig = new ConcurrentHashMap<>();

    public Map<String, Target> getTargets(Project project) {
        Map<String, Target> projectTargets = store.get(project);
        if (projectTargets == null) {
            ProjectType type = ProjectUtil.getType(project);
            switch (type) {
                case ANDROID_APP:
                    projectTargets = new HashMap<>();
                    for (BaseVariant v : project.getExtensions()
                            .getByType(AppExtension.class)
                            .getApplicationVariants()) {
                        projectTargets.put(v.getName(), new AndroidAppTarget(project, v.getName()));
                    }
                    break;
                case ANDROID_LIB:
                    projectTargets = new HashMap<>();
                    Map<String, Target> projectArtifacts = new HashMap<>();
                    String archiveBaseName = project.getConvention().getPlugin(BasePluginConvention.class)
                            .getArchivesBaseName();
                    for (BaseVariant v : project.getExtensions()
                            .getByType(LibraryExtension.class)
                            .getLibraryVariants()) {
                        Target target = new AndroidLibTarget(project, v.getName());
                        projectTargets.put(v.getName(), target);

                        projectArtifacts.put(v.getName(), target);
                    }
                    artifactNameToTarget.put(project, projectArtifacts);
                    break;
                case GROOVY_LIB:
                    projectTargets = Collections.singletonMap(JvmTarget.MAIN,
                            new GroovyLibTarget(project, JvmTarget.MAIN));
                    break;
                case KOTLIN_LIB:
                    projectTargets = Collections.singletonMap(JvmTarget.MAIN,
                            new KotlinLibTarget(project, JvmTarget.MAIN));
                    break;
                case SCALA_LIB:
                    projectTargets = Collections.singletonMap(JvmTarget.MAIN,
                            new ScalaLibTarget(project, JvmTarget.MAIN));
                    break;
                case JAVA_LIB:
                    projectTargets = Collections.singletonMap(JvmTarget.MAIN,
                            new JavaLibTarget(project, JvmTarget.MAIN));
                    break;
                default:
                    projectTargets = Collections.emptyMap();
                    break;
            }
            store.put(project, projectTargets);
        }

        return projectTargets;
    }

    @Nullable
    public Target getTargetForVariant(Project targetProject, String variant) {
        Target result;
        ProjectType type = ProjectUtil.getType(targetProject);
        switch (type) {
            case ANDROID_LIB:
                result = artifactNameToTarget.get(targetProject).get(variant);
                if (result == null) {
                    throw new IllegalStateException("No target found for " + targetProject
                            .getDisplayName() + " for variant " + variant);
                }
                break;
            case GROOVY_LIB:
            case JAVA_LIB:
            case KOTLIN_LIB:
            case SCALA_LIB:
                result = getTargets(targetProject).values().iterator().next();
                break;
            default:
                result = null;
        }
        return result;
    }

    public String lintConfig(Project project, File config) {
        String configName = FileUtil.getRelativePath(project.getRootDir(), config).replaceAll("/", "_");
        return lintConfig.computeIfAbsent(configName, key -> {
            File configFile = project.getRootProject().file(LINT_DEPS_CACHE + "/" + configName);
            try {
                FileUtils.copyFile(config, configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return "//" + LINT_DEPS_CACHE + ":" + configName;
        });
    }
}
