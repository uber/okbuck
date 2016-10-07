package com.uber.okbuck.printable

public class PostProcessClassessCommands implements Printable {

    private final List<String> mPostprocessClassesCommands;
    private final Set<String> mPostProcessDeps
    private final String mBootClasspath;
    private final String mGenDir;

    public PostProcessClassessCommands(String bootClasspath, String genDir, Set<String> postProcessDeps) {
        mPostprocessClassesCommands = []
        mBootClasspath = bootClasspath
        mGenDir = genDir
        mPostProcessDeps = postProcessDeps
    }

    public void addCommand(String command) {
        mPostprocessClassesCommands.add(command)
    }

    public void addCommands(List<String> commands) {
        mPostprocessClassesCommands.addAll(commands)
    }

    @Override
    public void print(PrintStream printer) {
        if (!mPostprocessClassesCommands.isEmpty()) {
            String deps = "\$(JARS=(`find ${mGenDir} ! -name \"*-abi.jar\" ! -name \"*dex.dex.jar\" -name \"*.jar\"`); " +
                    "CLASSPATH=${mBootClasspath}; IFS=:; MERGED=( \"\${JARS[@]}\" \"\${CLASSPATH[@]}\" ); " +
                    "echo \"\${MERGED[*]}\")"
            String commandClassPath = mPostProcessDeps.join(":")
            printer.println("\tpostprocess_classes_commands = [")
            mPostprocessClassesCommands.each {
                String command -> printer.println("\t\t'DEPS=${deps} COMMAND=${commandClassPath} ${command}',")
            }
            printer.println("\t],")
        }
    }
}
