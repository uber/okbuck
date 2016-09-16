package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.JavaTarget
import com.github.okbuilds.okbuck.rule.AptRule

final class AptRuleComposer extends JavaBuckRuleComposer {

    private AptRuleComposer() {
        // no instance
    }

    static AptRule compose(JavaTarget target) {
        Set<String> aptDeps = target.apt.externalDeps.findAll { String dep ->
            dep.endsWith(".jar")
        }
        return new AptRule(aptJar(target), external(aptDeps) as List)
    }
}
