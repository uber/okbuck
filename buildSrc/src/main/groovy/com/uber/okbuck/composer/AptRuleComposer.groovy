package com.uber.okbuck.composer

import com.uber.okbuck.core.model.JavaTarget
import com.uber.okbuck.rule.AptRule

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
