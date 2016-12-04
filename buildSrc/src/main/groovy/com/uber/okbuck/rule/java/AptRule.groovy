package com.uber.okbuck.rule.java

import com.uber.okbuck.rule.base.BuckRule

final class AptRule extends BuckRule {

    AptRule(String name, List<String> annotationProcessorDeps) {
        super("java_library", name, [], annotationProcessorDeps)
    }

    @Override
    protected void printContent(PrintStream printStream) {}
}
