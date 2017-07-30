package com.uber.okbuck.config;

import com.uber.okbuck.core.io.Printer;
import com.uber.okbuck.rule.base.BuckRule;

import java.util.List;

public final class BUCKFile extends BuckConfigFile {

    private final List<BuckRule> rules;

    public BUCKFile(List<BuckRule> rules) {
        this.rules = rules;
    }

    @Override
    public final void print(Printer printer) {
        for (BuckRule rule : rules) {
            rule.print(printer);
        }
    }
}
