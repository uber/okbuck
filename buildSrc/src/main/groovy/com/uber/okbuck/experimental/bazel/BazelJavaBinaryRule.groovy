package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.rule.BuckRule

final class BazelJavaBinaryRule extends BuckRule {
    private static final String RULE_TYPE = "java_binary"

    private final String mainClass
    private final String javaLibrary

    BazelJavaBinaryRule(String name, String javaLibrary, String mainClass) {
        super(RULE_TYPE, name)
        this.javaLibrary = javaLibrary
        this.mainClass = mainClass
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tmain_class = '${mainClass}',")
        printer.println("\truntime_deps = ['${javaLibrary}'],")
    }
}
