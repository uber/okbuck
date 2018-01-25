package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.template.android.GenAidlRule
import com.uber.okbuck.template.core.Rule

final class GenAidlRuleComposer extends AndroidBuckRuleComposer {

    private GenAidlRuleComposer() {
        // no instance
    }

    static Rule compose(AndroidTarget target, String aidlDir) {
        return new GenAidlRule()
                .aidlFilePath(aidlDir)
                .importPath("${target.path}/${aidlDir}")
                .manifest(fileRule(target.manifest))
                .aidlDeps(targets(target.main.targetDeps))
                .name(aidl(target, aidlDir))
    }
}
