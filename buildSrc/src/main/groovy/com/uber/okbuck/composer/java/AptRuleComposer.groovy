package com.uber.okbuck.composer.java

import com.uber.okbuck.core.model.java.JavaTarget
import com.uber.okbuck.rule.java.AptRule

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
