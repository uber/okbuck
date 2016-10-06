package com.uber.okbuck.printable;

public class PostProcessClassessCommands implements Printable {

    private final List<String> mPostprocessClassesCommands;
    private final String mBootClasspath;
    private final String mGenDir;

    public PostProcessClassessCommands(String bootClasspath, String genDir) {
        mPostprocessClassesCommands = []
        mBootClasspath = bootClasspath
        mGenDir = genDir
    }

    public void addCommand(String command) {
        mPostprocessClassesCommands.add(command)
    }

    public void addCommands(List<String> commands) {
        mPostprocessClassesCommands.addAll(commands)
    }

    @Override
    public void println(PrintStream printer) {
        if (!mPostprocessClassesCommands.isEmpty()) {
            String deps = "\$(JARS=(`find ${mGenDir} ! -name \"*-abi.jar\" ! -name \"*dex.dex.jar\" -name \"*.jar\"`); CLASSPATH=${mBootClasspath}; IFS=:; MERGED=( \"\${JARS[@]}\" \"\${CLASSPATH[@]}\" ); echo \"\${MERGED[*]}\")"
            printer.println("\tpostprocess_classes_commands = [")
            mPostprocessClassesCommands.each {
                String command -> printer.println("\t\t'DEPS=${deps} ${command}',")
            }
            printer.println("\t],")
        }
    }
}
