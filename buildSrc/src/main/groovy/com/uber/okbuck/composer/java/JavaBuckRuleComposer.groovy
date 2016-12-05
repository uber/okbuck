package com.uber.okbuck.composer.java

import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer
import com.uber.okbuck.core.model.java.JavaTarget

abstract class JavaBuckRuleComposer extends JvmBuckRuleComposer {

    static String bin(JavaTarget target) {
        return "bin_${target.name}"
    }

    static String aptJar(JavaTarget target) {
        return "apt_jar_${target.name}"
    }

    static String lint(JavaTarget target) {
        return "lint_${target.name}"
    }
}
