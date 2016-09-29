package com.uber.okbuck.composer

import com.uber.okbuck.rule.GradleSourceGenRule
import com.uber.okbuck.rule.ZipRule

final class ZipRuleComposer extends AndroidBuckRuleComposer {

    private ZipRuleComposer() {
        // no instance
    }

    static ZipRule compose(GradleSourceGenRule sourcegenRule) {
        return new ZipRule("${sourcegenRule.name}.src", [] as Set, [":${sourcegenRule.name}"] as Set)
    }
}
