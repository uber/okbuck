package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidTarget
import com.github.okbuilds.okbuck.rule.GenAidlRule

final class GenAidlRuleComposer extends AndroidBuckRuleComposer {

    private GenAidlRuleComposer() {
        // no instance
    }

    static GenAidlRule compose(AndroidTarget target, String aidlDir) {
        return new GenAidlRule(aidl(target), aidlDir, "${target.path}/${aidlDir}",
                targets(target.targetCompileDeps))
    }
}
