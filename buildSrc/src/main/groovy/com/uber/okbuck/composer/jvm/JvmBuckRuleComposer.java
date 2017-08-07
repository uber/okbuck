package com.uber.okbuck.composer.jvm;

import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.model.jvm.JvmTarget;

public class JvmBuckRuleComposer extends BuckRuleComposer {

    public static String src(final JvmTarget target) {
        return "src_" + target.getName();
    }

    public static String bin(final JvmTarget target) {
        return "bin_" + target.getName();
    }

    public static String test(final JvmTarget target) {
        return "test_" + target.getName();
    }
}
