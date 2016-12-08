package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.rule.android.GenAidlRule

final class GenAidlRuleComposer extends AndroidBuckRuleComposer {

    private GenAidlRuleComposer() {
        // no instance
    }

    static GenAidlRule compose(AndroidTarget target, String aidlDir) {
        return new GenAidlRule(aidl(target), aidlDir, "${target.path}/${aidlDir}", target.manifest,
                targets(target.main.targetDeps))
    }
}
