package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.core.model.JavaAppTarget
import com.uber.okbuck.composer.JavaBuckRuleComposer

final class BazelJavaBinaryRuleComposer extends JavaBuckRuleComposer {
    static BazelJavaBinaryRule compose(JavaAppTarget target) {
        return new BazelJavaBinaryRule(bin(target), ":${src(target)}", target.mainClass)
    }
}
