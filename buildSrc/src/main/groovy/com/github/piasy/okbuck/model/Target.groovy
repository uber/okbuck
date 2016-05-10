package com.github.piasy.okbuck.model

import com.github.piasy.okbuck.OkBuckExtension
import com.github.piasy.okbuck.OkBuckGradlePlugin
import com.github.piasy.okbuck.dependency.DependencyCache
import com.github.piasy.okbuck.dependency.ExternalDependency
import com.github.piasy.okbuck.util.FileUtil
import com.github.piasy.okbuck.util.ProjectUtil
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.UnknownConfigurationException

import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * A target is roughly equivalent to what can be built with gradle via the various assemble tasks.
 *
 * For a project with no flavors and three build types - debug, release and development,
 * the possible variants are debug, release and development.
 * For a project with flavors flavor1 and flavor2 and three build types - debug, release and
 * development, the possible variants are flavor1Debug, flavor1Release, flavor1Development,
 * flavor2Debug, flavor2Release, flavor2Development.
 *
 * This class encapsulates all the data related to a variant to generate config files.
 */
@ToString(includes = ['project', 'name'])
@EqualsAndHashCode(includes = ["project", "name"])
abstract class Target {

    static final Set<String> APT_CONFIGURATIONS = ["apt", "provided"] as Set
    static final String PROCESSOR_ENTRY =
            "META-INF/services/javax.annotation.processing.Processor"
    public static final String RETRO_LAMBDA_CONFIG = "retrolambdaConfig"

    final Project project
    final String name
    final String identifier
    final String path
    final Set<String> sources = [] as Set
    final Set<Target> targetAptDeps = [] as Set
    final Set<Target> targetCompileDeps = [] as Set
    final boolean mRetroLambdaEnabled

    protected final Project rootProject
    protected final Set<ExternalDependency> externalAptDeps = [] as Set
    protected final Set<ExternalDependency> externalCompileDeps = [] as Set

    /**
     * Constructor.
     *
     * @param project The project.
     * @param name The target name.
     */
    Target(Project project, String name) {
        this.project = project
        this.name = name

        identifier = project.path.replaceFirst(':', '')
        path = identifier.replaceAll(':', '/')

        rootProject = project.gradle.rootProject

        sources.addAll(getAvailable(sourceDirs()))

        extractConfigurations(APT_CONFIGURATIONS, externalAptDeps, targetAptDeps)
        OkBuckExtension okbuck = rootProject.okbuck
        targetAptDeps.retainAll(targetAptDeps.findAll { Target target ->
            target.getProp(okbuck.annotationProcessors, null) != null
        })

        extractConfigurations(compileConfigurations(), externalCompileDeps, targetCompileDeps)
        mRetroLambdaEnabled = retroLambdaEnabled()
    }

    /**
     * List of source directories.
     */
    protected abstract Set<File> sourceDirs()

    /**
     * List of compile configurations.
     */
    protected abstract Set<String> compileConfigurations()

    Set<String> getAnnotationProcessors() {
        OkBuckExtension okbuck = rootProject.okbuck
        return aptDeps.collect { String aptDep ->
            JarFile jar = new JarFile(new File(aptDep))
            jar.entries().findAll { JarEntry entry ->
                entry.name.equals(PROCESSOR_ENTRY)
            }.collect { JarEntry aptEntry ->
                IOUtils.toString(jar.getInputStream(aptEntry)).trim().split("\\n")
            }
        }.plus(targetAptDeps.collect { Target target ->
            (List<String>) target.getProp(okbuck.annotationProcessors, null)
        }.findAll { List<String> processors ->
            processors != null
        }).flatten() as List<String>
    }

    Set<String> getCompileDeps() {
        externalCompileDeps.collect { ExternalDependency dependency ->
            dependencyCache.get(dependency)
        }
    }

    Set<String> getAptDeps() {
        externalAptDeps.collect { ExternalDependency dependency ->
            dependencyCache.get(dependency)
        }
    }

    boolean getRetroLambdaEnabled() {
        return mRetroLambdaEnabled
    }

    protected Set<String> getAvailable(Collection<File> files) {
        return files.findAll { File file ->
            file.exists()
        }.collect { File file ->
            FileUtil.getRelativePath(project.projectDir, file)
        }
    }

    protected DependencyCache getDependencyCache() {
        return ((OkBuckGradlePlugin) rootProject.plugins
                .getPlugin(OkBuckGradlePlugin)).dependencyCache
    }

    private void extractConfigurations(Set<String> configurations, Set<ExternalDependency> externalConfigurationDeps,
                                       Set<Target> targetConfigurationDeps) {
        configurations.each { String configName ->
            try {
                Configuration configuration = project.configurations.getByName(configName)
                Set<File> resolvedFiles = [] as Set
                configuration.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact artifact ->
                    String identifier = artifact.id.componentIdentifier.displayName
                    File dep = artifact.file

                    resolvedFiles.add(dep)

                    if (identifier.contains(" ")) {
                        Target target = ProjectUtil.getTargetForOutput(rootProject, dep)
                        if (target != null) {
                            targetConfigurationDeps.add(target)
                        }
                    } else {
                        ExternalDependency dependency = new ExternalDependency(identifier, dep)
                        externalConfigurationDeps.add(dependency)
                        dependencyCache.put(dependency)
                    }
                }

                configuration.resolve().findAll { File resolved ->
                    !resolvedFiles.contains(resolved)
                }.each { File localDep ->
                    ExternalDependency dependency = new ExternalDependency(
                            "${identifier.replaceAll(':', '_')}:${FilenameUtils.getBaseName(localDep.name)}:1.0.0",
                            localDep)
                    externalConfigurationDeps.add(dependency)
                    dependencyCache.put(dependency)
                }

            } catch (UnknownConfigurationException ignored) {
            }
        }
    }

    private boolean retroLambdaEnabled() {
        try {
            Configuration configuration = project.configurations.getByName(RETRO_LAMBDA_CONFIG)
            Set<ExternalDependency> retrolambdaDeps = [] as Set
            configuration.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact artifact ->
                String identifier = artifact.id.componentIdentifier.displayName
                File dep = artifact.file
                retrolambdaDeps.add(new ExternalDependency(identifier, dep))
            }
            return !retrolambdaDeps.empty
        } catch (UnknownConfigurationException ignored) {
        }
        return false
    }

    /**
     * Magic part... almost by reverse engineering :(
     *
     * add all dependencies (their jar file) into RetroLambda compile classpath, it doesn't matter
     * if some of those files doesn't exist. translate pattern is find out by analysing buck-out
     * dir's content.
     * */
    public String getDependencyClasspath() {
        String classpath = ""
        Set<String> addedDep = new HashSet<>()
        compileDeps.each { String dep ->
            String depPath;
            if (dep.endsWith(".jar")) {
                depPath = "./buck-out/gen/" + dep + ".jar"
            } else {
                depPath = "./buck-out/gen/" + dep + "#aar_prebuilt_jar.jar"
            }
            if (!addedDep.contains(depPath)) {
                if (StringUtils.isEmpty(classpath)) {
                    classpath += depPath
                } else {
                    classpath += ":" + depPath
                }
                addedDep.add(depPath)
            }
        }
        targetCompileDeps.each { Target dep ->
            String depPath = ""
            if (dep instanceof JavaLibTarget) {
                depPath = "./buck-out/gen/" + dep.path + "/lib__src_" + dep.name +
                        "__output/src_" + dep.name + ".jar"
            } else if (dep instanceof AndroidLibTarget) {
                String jarPath = "./buck-out/gen/" + dep.path + "/lib__src_" +
                        dep.name + "__output/src_" + dep.name + ".jar"
                String rPath = "./buck-out/gen/" + dep.path + "/__src_" +
                        dep.name + "#dummy_r_dot_java_dummyrdotjava_output__/src_" +
                        dep.name + "#dummy_r_dot_java.jar"
                String buildConfigPath = "./buck-out/gen/" + dep.path +
                        "/lib__build_config_" + dep.name + "__output/build_config_" +
                        dep.name + ".jar"
                depPath = jarPath + ":" + rPath + ":" + buildConfigPath
            }
            if (!StringUtils.isEmpty(depPath) && !addedDep.contains(depPath)) {
                if (StringUtils.isEmpty(classpath)) {
                    classpath += depPath
                } else {
                    classpath += ":" + depPath
                }
                addedDep.add(depPath)
            }
        }
        return classpath
    }

    def getProp(Map map, defaultValue) {
        return map.get("${identifier}${name}", map.get(identifier, defaultValue))
    }
}
