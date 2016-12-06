package com.uber.okbuck.rule.base

import com.uber.okbuck.core.model.base.RuleType

final class GenRule extends BuckRule {

    private final Set<String> inputs
    private final Set<String> bashCmds
    private final boolean globSrcs
    private final String output
    private final boolean executable

    GenRule(String name,
            List<String> inputs,
            List<String> bashCmds,
            executable) {
        this(name, inputs, bashCmds, false, "${name}_out", executable)
    }

    GenRule(String name,
            List<String> inputs,
            List<String> bashCmds,
            boolean globSrcs = false,
            String output = "${name}_out",
            executable = false) {
        super(RuleType.GENRULE, name)
        this.inputs = inputs
        this.bashCmds = bashCmds
        this.globSrcs = globSrcs
        this.output = output
        this.executable = executable
    }

    @Override
    final void printContent(PrintStream printer) {
        if (!inputs.empty) {
            printer.println(globSrcs ? "\tsrcs = glob([" : "\tsrcs = [")
            for (String input : inputs) {
                printer.println("\t\t'${input}',")
            }
            printer.println(globSrcs ? "\t])," : "\t],")
        }

        printer.println("\tout = '${output}',")
        if (executable) {
            printer.println("\texecutable = True,")
        }
        printer.println("\tbash = '' \\")
        bashCmds.each {
            printer.println("\t'${it} ' \\")
        }
        printer.println("\t'',")
    }
}
