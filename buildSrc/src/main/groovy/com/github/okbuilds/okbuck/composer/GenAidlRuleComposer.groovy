package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidTarget
import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.rule.GenAidlRule

final class GenAidlRuleComposer {

    private GenAidlRuleComposer() {
        // no instance
    }

    static GenAidlRule compose(AndroidTarget target, String aidlDir) {
        return new GenAidlRule("${target.name}_aidls", aidlDir,
                "${target.path}/${aidlDir}",
                target.targetCompileDeps.collect { Target targetDep ->
                    "//${targetDep.path}:src_${targetDep.name}"
                } as Set)
    }
}
