package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.core.model.JavaAppTarget
import com.uber.okbuck.composer.JavaBuckRuleComposer

final class JavaBinaryRuleComposer extends JavaBuckRuleComposer {
    static JavaBinaryRule compose(JavaAppTarget target) {
        return new JavaBinaryRule(bin(target), ":${src(target)}", target.mainClass)
    }
}
