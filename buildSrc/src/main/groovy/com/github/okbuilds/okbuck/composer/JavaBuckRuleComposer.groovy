package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.JavaTarget

abstract class JavaBuckRuleComposer extends BuckRuleComposer {

    static String bin(JavaTarget target) {
        return "bin_${target.name}"
    }

    static String src(JavaTarget target) {
        return "src_${target.name}"
    }

    static String test(JavaTarget target) {
        return "test_${target.name}"
    }

    static String aptJar(JavaTarget target) {
        return "apt_jar_${target.name}"
    }
}
