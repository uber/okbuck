package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidTarget
import com.github.okbuilds.okbuck.rule.GenSqlDelightRule

final class GenSqlDelightRuleComposer extends AndroidBuckRuleComposer {

    private GenSqlDelightRuleComposer() {
        // no instance
    }

    static GenSqlDelightRule compose(AndroidTarget target) {
        return new GenSqlDelightRule("sqldelight_${target.name}", target.sqlDelightJar,
                "${target.project.projectDir}/${target.sqldelight.sources.getAt(0)}")
    }
}
