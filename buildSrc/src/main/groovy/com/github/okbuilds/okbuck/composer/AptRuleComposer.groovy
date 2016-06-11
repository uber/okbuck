package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.rule.AptRule

final class AptRuleComposer {

    private AptRuleComposer() {
        // no instance
    }

    static AptRule compose(Target target) {
        List<String> aptDeps = target.aptDeps.collect { String aptDep ->
            "//${aptDep.reverse().replaceFirst("/", ":").reverse()}"
        }

        return new AptRule("apt_jar_${target.name}", aptDeps)
    }
}
