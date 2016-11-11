package com.uber.okbuck.generator

import com.uber.okbuck.constant.BuckConstants
import com.uber.okbuck.core.model.AndroidTarget
import com.uber.okbuck.core.model.JavaLibTarget
import com.uber.okbuck.core.util.FileUtil

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
        outputText = outputText.replaceAll('gen-dir', target.rootProject.file(BuckConstants.DEFAULT_BUCK_OUT_GEN_PATH).absolutePath)
                .replaceAll('retrolambda-jar', target.retroLambdaJar)
        if (target instanceof AndroidTarget) {
            outputText = outputText.replaceAll('android-jar', target.bootClasspath ?: "")
        } else {
            outputText = outputText.replaceAll('android-jar', "")
        }

        output.text = outputText

        output.setExecutable(true)
        return "./${FileUtil.getRelativePath(target.rootProject.projectDir, output)}"
    }
}
