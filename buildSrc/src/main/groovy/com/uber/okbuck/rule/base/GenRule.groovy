package com.uber.okbuck.rule.base

import com.uber.okbuck.core.io.Printer
import com.uber.okbuck.core.model.base.RuleType

final class GenRule extends BuckRule {

    private final Rule genrule

    GenRule(String name,
            List<String> inputs,
            List<String> bashCmds,
            boolean globSrcs = false,
            String output = "${name}_out",
            boolean executable = false) {
        super(RuleType.GENRULE, name)
        genrule = new template.base.GenRule()
                .inputs(inputs)
                .bashCmds(bashCmds.collect { it as String })
                .globSrcs(globSrcs)
                .output(output)
                .executable(executable)
                .ruleType(RuleType.GENRULE.name().toLowerCase())
                .name(name)
    }

    @Override
    void print(Printer printer) {
        printer.println(genrule.render().toString())
    }

    @Override
    void printContent(Printer printer) {}
}
