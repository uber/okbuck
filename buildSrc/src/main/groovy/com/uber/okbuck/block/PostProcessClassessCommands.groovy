package com.uber.okbuck.block;

public class PostProcessClassessCommands {

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

    public boolean isEmpty() {
        return mPostprocessClassesCommands.isEmpty();
    }

    public String buildCommand() {
        String deps = "\$(JARS=(`find ${mGenDir} ! -name \"*-abi.jar\" ! -name \"*dex.dex.jar\" -name \"*.jar\"`); IFS=:; echo \"\${JARS[*]}\")"
        String androidJar = mBootClasspath

        StringBuilder sb = new StringBuilder()
        sb.append("\tpostprocess_classes_commands = [\n")
        mPostprocessClassesCommands.each {
            String command -> sb.append("\t\t'export DEPS=${deps}; export ANDROID_JAR=${androidJar}; ${command}'\n,")
        }
        sb.append("\t],")
        return sb.toString();
    }
}
