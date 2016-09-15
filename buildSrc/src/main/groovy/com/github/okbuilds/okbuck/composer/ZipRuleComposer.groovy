package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.okbuck.rule.GradleSourceGenRule
import com.github.okbuilds.okbuck.rule.ZipRule

final class ZipRuleComposer extends AndroidBuckRuleComposer {

    private ZipRuleComposer() {
        // no instance
    }

    static ZipRule compose(GradleSourceGenRule sourcegenRule) {
        return new ZipRule("${sourcegenRule.name}.src", [] as Set, [":${sourcegenRule.name}"] as Set)
    }
}
