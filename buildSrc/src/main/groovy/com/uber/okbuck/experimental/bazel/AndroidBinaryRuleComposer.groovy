package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.composer.AndroidBuckRuleComposer
import com.uber.okbuck.core.model.AndroidAppTarget

final class AndroidBinaryRuleComposer extends AndroidBuckRuleComposer {
    static AndroidBinaryRule compose(AndroidAppTarget target) {
        return new AndroidBinaryRule(
                bin(target),
                target.getPackage(),
                ":${src(target)}",
                target.multidexEnabled,
                target.manifest,
                target.placeholders)
    }
}
