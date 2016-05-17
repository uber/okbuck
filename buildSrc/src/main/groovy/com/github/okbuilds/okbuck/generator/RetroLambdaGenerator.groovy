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

package com.github.okbuilds.okbuck.generator

import com.github.okbuilds.core.model.JavaLibTarget
import com.github.okbuilds.core.util.FileUtil

final class RetroLambdaGenerator {

    private RetroLambdaGenerator() {}

    /**
     * Return the path to a generated retrolambda script
     */
    static String generate(JavaLibTarget target) {
        File output =
                new File(".okbuck/retrolambda/${target.identifier.replaceAll(':', '_')}_${target.name}_RetroLambda.sh")
        output.parentFile.mkdirs()
        output.createNewFile()

        FileUtil.copyResourceToProject("retrolambda/RetroLambda.sh", output)

        String outputText = output.text
        outputText = outputText.replaceAll('gen-dir', target.rootProject.file("buck-out/gen").absolutePath)
                .replaceAll('retrolambda-jar', target.retroLambdaJar)
            outputText = outputText.replaceAll('android-jar', target.bootClasspath)

        output.text = outputText

        output.setExecutable(true)
        return "./${FileUtil.getRelativePath(target.rootProject.projectDir, output)}"
    }
}
