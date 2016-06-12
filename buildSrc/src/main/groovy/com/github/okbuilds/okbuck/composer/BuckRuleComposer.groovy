package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.Target

abstract class BuckRuleComposer {

    static Set<String> external(Set<String> deps) {
        return deps.collect { String dep ->
            "//${dep.reverse().replaceFirst("/", ":").reverse()}"
        }
    }

    static Set<String> targets(Set<Target> deps) {
        return deps.collect { Target targetDep ->
            "//${targetDep.path}:src_${targetDep.name}"
        }
    }
}
