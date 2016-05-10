/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.piasy.okbuck

import com.github.piasy.okbuck.config.BUCKFile
import com.github.piasy.okbuck.config.RetroLambdaShFile
import com.github.piasy.okbuck.dependency.DependencyCache
import com.github.piasy.okbuck.dependency.ExternalDependency
import com.github.piasy.okbuck.generator.BuckFileGenerator
import com.github.piasy.okbuck.generator.DotBuckConfigGenerator
import com.github.piasy.okbuck.model.AndroidTarget
import com.github.piasy.okbuck.model.Target
import com.github.piasy.okbuck.util.ProjectUtil
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.UnknownConfigurationException

class OkBuckGradlePlugin implements Plugin<Project> {

    static final String OKBUCK = "okbuck"
    static final String OKBUCK_CLEAN = 'okbuckClean'
    static final String BUCK = "BUCK"

    DependencyCache dependencyCache

    void apply(Project project) {
        OkBuckExtension okbuck = project.extensions.create(OKBUCK, OkBuckExtension, project)

        dependencyCache = new DependencyCache(project)

        Task okBuckClean = project.task(OKBUCK_CLEAN)
        okBuckClean << {
            if (okbuck.overwrite) {
                [".okbuck", ".buckd", "buck-out", ".buckconfig"].each { String file ->
                    FileUtils.deleteQuietly(project.file(file))
                }
                okbuck.buckProjects.each { Project buckProject ->
                    FileUtils.deleteQuietly(buckProject.file(BUCK))
                }
            }
        }

        Task okBuck = project.task(OKBUCK)
        okBuck.outputs.upToDateWhen { false }
        okBuck.dependsOn(okBuckClean)
        okBuck << {
            generate(project)
        }
    }

    private static generate(Project project) {
        OkBuckExtension okbuck = project.okbuck

        if (okbuck.overwrite) {
            println "==========>> okbuck overwrite mode is on <<=========="
        }

        // generate .buckconfig
        File dotBuckConfig = project.file(".buckconfig")
        if (dotBuckConfig.exists() && !okbuck.overwrite) {
            throw new IllegalStateException(".buckconfig already exists, set `overwrite=true` to regenerate.")
        } else {
            PrintStream printer = new PrintStream(dotBuckConfig)
            DotBuckConfigGenerator.generate(okbuck).print(printer)
            IOUtils.closeQuietly(printer)
        }

        ExternalDependency latestRetroLambda = null
        project.subprojects.each {
            try {
                Configuration configuration = it.configurations.getByName(
                        Target.RETRO_LAMBDA_CONFIG)
                configuration.resolvedConfiguration.resolvedArtifacts.each {
                    ResolvedArtifact artifact ->
                        String identifier = artifact.id.componentIdentifier.displayName
                        File dep = artifact.file
                        ExternalDependency retroLambda = new ExternalDependency(identifier, dep)
                        if (latestRetroLambda == null ||
                                latestRetroLambda.version.compareTo(retroLambda.version) < 0) {
                            latestRetroLambda = retroLambda
                        }
                }
            } catch (UnknownConfigurationException ignored) {
            }
        }

        if (latestRetroLambda != null) {
            File retroLambdaDir = new File(project.projectDir, ".okbuck/RetroLambda")
            retroLambdaDir.mkdirs()
            FileUtils.copyFile(latestRetroLambda.depFile,
                    new File(retroLambdaDir, latestRetroLambda.depFile.name))
            String classpath = ""
            int targetSdk = 0
            project.subprojects.each {
                ProjectUtil.getTargets(it).each { String name, Target target ->
                    classpath += target.dependencyClasspath
                    if (target instanceof AndroidTarget && ((AndroidTarget) target).targetSdk > targetSdk) {
                        targetSdk = ((AndroidTarget) target).targetSdk
                    }
                }
            }
            File retroLambdaShFile = new File(retroLambdaDir, "RetroLambda.sh")
            def printer = new PrintStream(retroLambdaShFile)
            new RetroLambdaShFile(classpath, "android-${targetSdk}",
                    latestRetroLambda.depFile.name).print(printer)
            IOUtils.closeQuietly(printer)
            retroLambdaShFile.setExecutable(true)
        }

        // generate BUCK file for each project
        Map<Project, BUCKFile> buckFiles = new BuckFileGenerator(project).generate()

        buckFiles.each { Project subProject, BUCKFile buckFile ->
            def printer = new PrintStream(subProject.file(BUCK))
            buckFile.print(printer)
            IOUtils.closeQuietly(printer)
        }
    }
}
