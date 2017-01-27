package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.rule.base.BuckRule

final class BazelJavaBinaryRule extends BuckRule {
    private static final RuleType RULE_TYPE = RuleType.JAVA_BINARY

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
