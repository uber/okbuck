package com.uber.okbuck.printable

import com.uber.okbuck.constant.BuckConstants
import com.uber.okbuck.core.model.JavaLibTarget
import com.uber.okbuck.core.util.FileUtil

public class PostProcessClassessCommands implements Printable {

    private static final DEPENDENCIES_CLASSPATH = "DEPENDENCIES_CLASSPATH"
    private static final POSTPROCESS_CLASSPATH = "POSTPROCESS_CLASSPATH"

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
        String suffix = command
                .replace("/", "_").replace(".", "_").replace(":", "_").replace(" ", "_").replace("\\", "_")
        File output = new File(
                ".okbuck/postprocess/${mTarget.identifier.replaceAll(':', '_')}_${mTarget.name}_${suffix}_postprocess.sh")
        output.parentFile.mkdirs()
        output.createNewFile()

        List<String> depsFindConstraints = []
        depsFindConstraints.add("! -name \"*abi.jar\"")
        depsFindConstraints.add("! -name \"*#dummy*.jar\"")
        depsFindConstraints.add("! -name \"*dex.dex.jar\"")
        depsFindConstraints.add("-name \"*.jar\"")
        String deps = "\$(JARS=(`find ${mGenDir} ${depsFindConstraints.join(" ")}`); " +
                "CLASSPATH=${mBootClasspath}; IFS=:; MERGED=( \"\${JARS[@]}\" \"\${CLASSPATH[@]}\" ); " +
                "echo \"\${MERGED[*]}\")"
        String commandClassPath = mPostProcessDeps.join(":")

        FileWriter writer = new FileWriter(output)
        writer.append("${DEPENDENCIES_CLASSPATH}=${deps} ${POSTPROCESS_CLASSPATH}=${commandClassPath} ${command} \$1")
        writer.close()

        output.setExecutable(true)
        return "./${FileUtil.getRelativePath(mTarget.rootProject.projectDir, output)}"
    }
}
