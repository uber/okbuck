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

package com.github.okbuilds.okbuck

import com.github.okbuilds.core.dependency.DependencyCache
import com.github.okbuilds.okbuck.config.BUCKFile
import com.github.okbuilds.okbuck.generator.BuckFileGenerator
import com.github.okbuilds.okbuck.generator.DotBuckConfigLocalGenerator
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

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
            [".okbuck", ".buckd", "buck-out", ".buckconfig.local"]
                    .plus(okbuck.buckProjects.collect { it.file(BUCK).absolutePath })
                    .minus(okbuck.keep).each { String file ->
                FileUtils.deleteQuietly(project.file(file))
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

        // generate empty .buckconfig if it does not exist
        File dotBuckConfig = project.file(".buckconfig")
        if (!dotBuckConfig.exists()) {
            dotBuckConfig.createNewFile()
        }

        // generate .buckconfig.local
        File dotBuckConfigLocal = project.file(".buckconfig.local")
        PrintStream configPrinter = new PrintStream(dotBuckConfigLocal)
        DotBuckConfigLocalGenerator.generate(okbuck).print(configPrinter)
        IOUtils.closeQuietly(configPrinter)

        // generate BUCK file for each project
        Map<Project, BUCKFile> buckFiles = new BuckFileGenerator(project).generate()

        buckFiles.each { Project subProject, BUCKFile buckFile ->
            PrintStream buckPrinter = new PrintStream(subProject.file(BUCK))
            buckFile.print(buckPrinter)
            IOUtils.closeQuietly(buckPrinter)
        }
    }
}
