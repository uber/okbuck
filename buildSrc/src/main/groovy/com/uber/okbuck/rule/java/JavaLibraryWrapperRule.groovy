package com.uber.okbuck.rule.java

import com.uber.okbuck.core.io.Printer
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.rule.base.BuckRule

final class JavaLibraryWrapperRule extends BuckRule {

    JavaLibraryWrapperRule(String name, List<String> deps) {
        super(RuleType.JAVA_LIBRARY, name, [], deps)
    }

    @Override
    protected void printContent(Printer printStream) {}
}
