package com.uber.okbuck.composer

import com.uber.okbuck.core.model.AndroidTarget
import com.uber.okbuck.rule.GenAidlRule

final class GenAidlRuleComposer extends AndroidBuckRuleComposer {

    private GenAidlRuleComposer() {
        // no instance
    }

    static GenAidlRule compose(AndroidTarget target, String aidlDir) {
        return new GenAidlRule(aidl(target), aidlDir, "${target.path}/${aidlDir}",
                targets(target.main.targetDeps))
    }
}
