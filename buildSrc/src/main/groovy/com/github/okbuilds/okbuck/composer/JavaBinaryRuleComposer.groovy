package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.JavaAppTarget
import com.github.okbuilds.okbuck.rule.JavaBinaryRule

final class JavaBinaryRuleComposer extends JavaBuckRuleComposer {

    private JavaBinaryRuleComposer() {
        // no instance
    }

    static JavaBinaryRule compose(JavaAppTarget target) {
        return new JavaBinaryRule(bin(target), ["PUBLIC"], [":${src(target)}"], target.mainClass)
    }
}
