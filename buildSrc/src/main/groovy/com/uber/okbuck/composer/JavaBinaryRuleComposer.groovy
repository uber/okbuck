package com.uber.okbuck.composer

import com.uber.okbuck.core.model.JavaAppTarget
import com.uber.okbuck.rule.JavaBinaryRule

final class JavaBinaryRuleComposer extends JavaBuckRuleComposer {

    private JavaBinaryRuleComposer() {
        // no instance
    }

    static JavaBinaryRule compose(JavaAppTarget target) {
        return new JavaBinaryRule(bin(target), ["PUBLIC"], [":${src(target)}"], target.mainClass, target.excludes)
    }
}
