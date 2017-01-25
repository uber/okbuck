package com.uber.okbuck.rule.android

import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.rule.base.BuckRule

final class AndroidLibraryWrapperRule extends BuckRule {

    AndroidLibraryWrapperRule(String name, List<String> deps) {
        super(RuleType.ANDROID_LIBRARY, name, [], deps)
    }

    @Override
    protected void printContent(PrintStream printStream) {}
}
