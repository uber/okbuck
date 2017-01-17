package com.uber.okbuck.composer.java;

import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer;
import com.uber.okbuck.core.model.java.JavaTarget;

public abstract class JavaBuckRuleComposer extends JvmBuckRuleComposer {

    protected static String bin(final JavaTarget target) {
        return "bin_" + target.getName();
    }

    protected static String aptJar(final JavaTarget target) {
        return "apt_jar_" + target.getName();
    }
}
