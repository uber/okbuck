package com.uber.okbuck.composer

import com.uber.okbuck.core.model.Target

abstract class BuckRuleComposer {

    static final String SEPARATOR = ':'

    static Set<String> external(Set<String> deps) {
        return deps.collect { String dep ->
            external(dep)
        }
    }

    static String external(String dep) {
        return "//${dep.reverse().replaceFirst("/", ":").reverse()}"
    }

    static Set<String> targets(Set<Target> deps) {
        return deps.collect { Target targetDep ->
            targets(targetDep)
        }
    }

    static String targets(Target dep) {
        return "//${dep.path}:src_${dep.name}"
    }

    static String binTargets(Target dep) {
        return "//${dep.path}:bin_${dep.name}"
    }

    static String toLocation(List<String> targets) {
        return (targets.collect { toLocation(it) }).join(SEPARATOR)
    }

    static String toLocation(String target) {
        return "\$(location ${target})"
    }
}
