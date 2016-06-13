package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.rule.AptRule

final class AptRuleComposer extends JavaBuckRuleComposer {

    private AptRuleComposer() {
        // no instance
    }

    static AptRule compose(Target target) {
        return new AptRule(aptJar(target), external(target.apt.externalDeps) as List)
    }
}
