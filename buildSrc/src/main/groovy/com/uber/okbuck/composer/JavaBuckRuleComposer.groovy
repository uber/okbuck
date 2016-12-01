package com.uber.okbuck.composer

import com.uber.okbuck.core.model.JavaTarget

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

    static String lint(JavaTarget target) {
        return "lint_${target.name}"
    }

    static String transform(String runnerClass, JavaTarget target) {
        return "transform_${runnerClass.replace(".", "_")}_${target.name}"
    }
}
