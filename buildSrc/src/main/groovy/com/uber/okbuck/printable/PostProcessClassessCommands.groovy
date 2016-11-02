package com.uber.okbuck.printable

import com.uber.okbuck.constant.BuckConstants
import com.uber.okbuck.core.model.JavaLibTarget
import com.uber.okbuck.core.util.FileUtil

public class PostProcessClassessCommands implements Printable {

    private static final DEPENDENCIES_CLASSPATH_FILE = "DEPENDENCIES_CLASSPATH_FILE"
    private static final POSTPROCESS_CLASSPATH_FILE = "POSTPROCESS_CLASSPATH_FILE"

    private final List<String> mPostprocessClassesCommands;
    private final Set<String> mPostProcessDeps
    private final String mBootClasspath;
    private final String mGenDir;
    private final JavaLibTarget mTarget;

    public PostProcessClassessCommands(JavaLibTarget target,
                                       Set<String> postProcessDeps,
                                       List<String> postprocessClassesCommands = []) {
        mBootClasspath = target.bootClasspath;
        mGenDir = target.rootProject.file(BuckConstants.DEFAULT_BUCK_OUT_GEN_PATH).absolutePath
        mPostprocessClassesCommands = postprocessClassesCommands
        mPostProcessDeps = postProcessDeps
        mTarget = target;
    }

    @Override
    public void print(PrintStream printer) {
        if (!mPostprocessClassesCommands.isEmpty()) {
            printer.println("\tpostprocess_classes_commands = [")
            mPostprocessClassesCommands.each {
                String command -> printer.println("\t\t'${generate(command)}',")
            }
            printer.println("\t],")
        }
    }

    private String generate(String command) {
        String suffix = command.replace("/", "_").replace(".", "_").replace(":", "_").replace(" ", "_").replace("\\", "_")
        String baseFileName = ".okbuck/postprocess/${mTarget.identifier.replaceAll(':', '_')}_${mTarget.name}_${suffix}_postprocess";

        String postProcessPath = writeFile("${baseFileName}_postProcessDeps", mPostProcessDeps.join(":")).getAbsolutePath()
        String dependenciesPath = ".okbuck/postprocess/${mTarget.identifier.replaceAll(':', '_')}_deps"

        File scriptFile = writeFile("${baseFileName}.sh",
                "#!/bin/bash\n" +
                "set -x \n" +
                "set -e \n" +
                "${generateFindDepsCommand(dependenciesPath)} \n",
                "${DEPENDENCIES_CLASSPATH_FILE}=${dependenciesPath} ${POSTPROCESS_CLASSPATH_FILE}=${postProcessPath} ${command} \$1 \n")
        scriptFile.setExecutable(true)

        return "./${FileUtil.getRelativePath(mTarget.rootProject.projectDir, scriptFile)}"
    }

    private String generateFindDepsCommand(String outputFile) {
        List<String> depsFindConstraints = []
        depsFindConstraints.add("! -name \"*abi.jar\"")
        depsFindConstraints.add("! -name \"*#dummy*.jar\"")
        depsFindConstraints.add("! -name \"*dex.dex.jar\"")
        depsFindConstraints.add("-name \"*.jar\"")
        StringBuilder sb = new StringBuilder()

        sb.append("if [ ! -f ${outputFile} ]; then \n")
        sb.append("\tfind ${mGenDir} ${depsFindConstraints.join(" ")} | tr '\\n' ':'> ${outputFile} \n")
        sb.append("\techo \"${mBootClasspath}\" >> ${outputFile} \n")
        sb.append("fi \n")

        return sb.toString()
    }

    private File writeFile(String filename, String... strings) {
        File output = new File(filename)
        output.parentFile.mkdirs()
        output.createNewFile()

        FileWriter writer = new FileWriter(output)
        for (String s : strings) {
            writer.append(s)
        }
        writer.close()

        return output
    }

}
