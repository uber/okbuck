package com.uber.okbuck.composer.jvm

import com.uber.okbuck.composer.base.BuckRuleComposer
import com.uber.okbuck.core.model.jvm.JvmTarget

class JvmBuckRuleComposer extends BuckRuleComposer {

    static String src(JvmTarget target) {
        return "src_${target.name}"
    }

    static String test(JvmTarget target) {
        return "test_${target.name}"
    }
}
