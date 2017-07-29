package com.uber.okbuck.core.util;

import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.LibraryPlugin;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.model.base.ProjectType;
import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.model.base.TargetCache;
import com.uber.okbuck.extension.OkBuckExtension;

import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.plugins.scala.ScalaPlugin;
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper;

import java.io.File;
import java.util.Map;

public final class ProjectUtil {

    private ProjectUtil() {}

    public static ProjectType getType(Project project) {
        PluginContainer plugins = project.getPlugins();
        if (plugins.hasPlugin(AppPlugin.class)) {
            return ProjectType.ANDROID_APP;
        } else if (plugins.hasPlugin(LibraryPlugin.class)) {
            return ProjectType.ANDROID_LIB;
        } else if (plugins.hasPlugin(GroovyPlugin.class)) {
            return ProjectType.GROOVY_LIB;
        } else if (plugins.hasPlugin(KotlinPluginWrapper.class)) {
            return ProjectType.KOTLIN_LIB;
        } else if (plugins.hasPlugin(ScalaPlugin.class)) {
            return ProjectType.SCALA_LIB;
        } else if (plugins.hasPlugin(JavaPlugin.class)) {
            return ProjectType.JAVA_LIB;
        } else {
            return ProjectType.UNKNOWN;
        }
    }

    public static DependencyCache getDependencyCache(Project project) {
        return getPlugin(project).depCache;
    }

    public static Map<String, Target> getTargets(Project project) {
        return getTargetCache(project).getTargets(project);
    }

    public static Target getTargetForOutput(Project targetProject, File output) {
        return getTargetCache(targetProject).getTargetForOutput(targetProject, output);
    }

    public static OkBuckExtension getExtension(Project project) {
        return project.getRootProject().getExtensions().getByType(OkBuckExtension.class);
    }

    static OkBuckGradlePlugin getPlugin(Project project) {
        return project.getRootProject().getPlugins().getPlugin(OkBuckGradlePlugin.class);
    }

    private static TargetCache getTargetCache(Project project) {
        return getPlugin(project).targetCache;
    }

    static String findVersionInClasspath(Project project, String group, String module) {
        return project.getBuildscript()
                .getConfigurations()
                .getByName("classpath")
                .getIncoming()
                .getArtifacts()
                .getArtifacts()
                .stream()
                .filter(resolvedArtifactResult -> {
                    ModuleComponentIdentifier identifier =
                            (ModuleComponentIdentifier) resolvedArtifactResult.getId().getComponentIdentifier();
                    return (group.equals(identifier.getGroup()) &&
                            module.equals(identifier.getModule()));
                })
                .findFirst()
                .map(r -> ((ModuleComponentIdentifier) r.getId().getComponentIdentifier()).getVersion())
                .orElse(null);
    }
}
