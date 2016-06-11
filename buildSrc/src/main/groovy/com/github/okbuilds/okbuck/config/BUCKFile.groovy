package com.github.okbuilds.okbuck.config

import com.github.okbuilds.okbuck.rule.BuckRule

final class BUCKFile extends BuckConfigFile {

    private final List<BuckRule> mRules

    BUCKFile(List<BuckRule> rules) {
        mRules = rules
    }

    @Override
    final void print(PrintStream printer) {
        mRules.each { rule ->
            rule.print(printer)
        }
    }
}
