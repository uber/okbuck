package com.uber.okbuck.rule.java

import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.rule.base.BuckRule

final class AptRule extends BuckRule {

    AptRule(String name, List<String> annotationProcessorDeps) {
        super(RuleType.JAVA_LIBRARY, name, [], annotationProcessorDeps)
    }

    @Override
    protected void printContent(PrintStream printStream) {}
}
