package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.composer.android.AndroidBuckRuleComposer
import com.uber.okbuck.core.model.android.AndroidAppTarget

final class BazelAndroidBinaryRuleComposer extends AndroidBuckRuleComposer {
    static BazelAndroidBinaryRule compose(AndroidAppTarget target) {
        return new BazelAndroidBinaryRule(
                bin(target),
                target.getPackage(),
                ":${src(target)}",
                target.multidexEnabled,
                target.manifest,
                target.placeholders)
    }
}
