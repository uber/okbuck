package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.okbuck.rule.GradleSourcegenRule
import com.github.okbuilds.okbuck.rule.ZipRule

final class ZipRuleComposer extends AndroidBuckRuleComposer {

    private ZipRuleComposer() {
        // no instance
    }

    static ZipRule compose(GradleSourcegenRule sourcegenRule) {
        return new ZipRule("${sourcegenRule.name}.src", [] as Set, [":${sourcegenRule.name}"] as Set)
    }
}
