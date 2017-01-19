package com.uber.okbuck.composer.java

import com.uber.okbuck.core.model.java.JavaTarget
import com.uber.okbuck.rule.java.AptRule

final class AptRuleComposer extends JavaBuckRuleComposer {

    private AptRuleComposer() {
        // no instance
    }

    static AptRule compose(JavaTarget target) {
        Set<String> aptDeps = external(target.apt.externalDeps.findAll { String dep ->
            dep.endsWith(".jar")
        })
        aptDeps += targets(target.apt.targetDeps)
        return new AptRule(aptJar(target), aptDeps as List)
    }
}
