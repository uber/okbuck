package com.uber.okbuck.composer

import com.uber.okbuck.rule.GenRule
import com.uber.okbuck.rule.ZipRule

final class ZipRuleComposer extends AndroidBuckRuleComposer {

    private ZipRuleComposer() {
        // no instance
    }

    static ZipRule compose(GenRule sourcegenRule) {
        return new ZipRule("${sourcegenRule.name}.src", [] as Set, [":${sourcegenRule.name}"] as Set)
    }
}
