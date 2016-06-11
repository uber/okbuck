package com.github.okbuilds.okbuck.rule

final class AptRule extends BuckRule {

    AptRule(String name, List<String> annotationProcessorDeps) {
        super("java_library", name, [], annotationProcessorDeps)
    }

    @Override
    protected void printContent(PrintStream printStream) {}
}
