package com.uber.okbuck.core.util;

import java.io.File;
import java.util.Map;
import java.util.stream.Stream;

import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.LibraryPlugin;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.model.base.ProjectType;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.model.base.TargetCache;
import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.plugins.scala.ScalaPlugin;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper;

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

    @Nullable
    public static Target getTargetForOutput(Project targetProject, File output) {
        return getTargetCache(targetProject).getTargetForOutput(targetProject, output);
    }

    public static Map<Project, Map<String, Scope>> getScopes(Project project) {
        return getPlugin(project).scopes;
    }

    static OkBuckGradlePlugin getPlugin(Project project) {
        return project.getRootProject().getPlugins().getPlugin(OkBuckGradlePlugin.class);
    }

    private static TargetCache getTargetCache(Project project) {
        return getPlugin(project).targetCache;
    }

    @Nullable
    static String findVersionInClasspath(Project project, String group, String module) {
        return project.getBuildscript()
                .getConfigurations()
                .getByName("classpath")
                .getIncoming()
                .getArtifacts()
                .getArtifacts()
                .stream()
                .flatMap(artifactResult ->
                    artifactResult.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier
                        ? Stream.of((ModuleComponentIdentifier) artifactResult.getId().getComponentIdentifier())
                        : Stream.empty())
                .filter(identifier -> (group.equals(identifier.getGroup()) &&
                        module.equals(identifier.getModule())))
                .findFirst()
                .map(ModuleComponentIdentifier::getVersion)
                .orElse(null);
    }
}
