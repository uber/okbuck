package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.composer.java.JavaBuckRuleComposer
import com.uber.okbuck.core.model.java.JavaAppTarget

final class BazelJavaBinaryRuleComposer extends JavaBuckRuleComposer {
    static BazelJavaBinaryRule compose(JavaAppTarget target) {
        return new BazelJavaBinaryRule(bin(target), ":${src(target)}", target.mainClass)
    }
}
