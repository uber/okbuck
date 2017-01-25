package com.uber.okbuck.composer.java

import com.uber.okbuck.core.model.java.JavaTarget
import com.uber.okbuck.rule.java.JavaLibraryWrapperRule

final class AptRuleComposer extends JavaBuckRuleComposer {

    private AptRuleComposer() {
        // no instance
    }

    static JavaLibraryWrapperRule compose(JavaTarget target) {
        Set<String> aptDeps = external(target.apt.externalDeps.findAll { String dep ->
            dep.endsWith(".jar")
        })
        aptDeps += targets(target.apt.targetDeps)
        return new JavaLibraryWrapperRule(aptJar(target), aptDeps as List)
    }
}
