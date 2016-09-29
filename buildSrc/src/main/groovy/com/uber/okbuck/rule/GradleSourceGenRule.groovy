package com.uber.okbuck.rule

final class GradleSourceGenRule extends BuckRule {

    private final String mRootProjectPath
    private final String mGradlePath
    private final String mGradleTask
    private final Set<String> mInputs
    private final String mOutputDir

    GradleSourceGenRule(String name, String rootProjectPath, String gradlePath, String gradleTask, Set<String> inputs, String outputDir) {
        super("genrule", name)
        mRootProjectPath = rootProjectPath
        mGradlePath = gradlePath
        mGradleTask = gradleTask
        mOutputDir = outputDir
        mInputs = inputs
    }

    @Override
    final void printContent(PrintStream printer) {
        if (!mInputs.empty) {
            printer.println("\tsrcs = glob([")
            for (String input : mInputs) {
                printer.println("\t\t'${input}',")
            }
            printer.println("\t]),")
        }
        printer.println("\tout = '${name}_out',")
        printer.println("\tbash = '${mGradlePath} -p ${mRootProjectPath} ${mGradleTask}" +
                " --stacktrace && cp -a ${mOutputDir} \$OUT',")
    }
}
