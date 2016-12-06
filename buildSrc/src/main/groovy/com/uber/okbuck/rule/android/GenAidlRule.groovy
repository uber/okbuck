package com.uber.okbuck.rule.android

import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.rule.base.BuckRule

final class GenAidlRule extends BuckRule {

    private final String mAidlFilePath
    private final String mImportPath
    private final Set<String> mAidlDeps

    GenAidlRule(String name, String aidlFilePath, String importPath, Set<String> deps) {
        super(RuleType.GEN_AIDL, name)
        mAidlFilePath = aidlFilePath
        mImportPath = importPath
        mAidlDeps = deps
    }

    @Override
    final void print(PrintStream printer) {
        printer.println("import re")
        printer.println("gen_${name} = []")
        printer.println("for aidl_file in glob(['${mAidlFilePath}/**/*.aidl']):")
        printer.println("\tname = '${name}__' + re.sub(r'^.*/([^/]+)\\.aidl\$', r'\\1', aidl_file)")
        printer.println("\tgen_${name}.append(':' + name)")
        printer.println("\tgen_aidl(")
        printer.println("\t\tname = name,")
        printer.println("\t\taidl = aidl_file,")
        printer.println("\t\timport_path = '${mImportPath}',")
        printer.println("\t)")
        printer.println()

        printer.println("android_library(")
        printer.println("\tname = '${name}',")
        printer.println("\tsrcs = gen_${name},")
        printer.println("\tdeps = [")
        mAidlDeps.each { String aidlDep ->
            printer.println("\t\t'${aidlDep}',")
        }
        printer.println("\t],")
        printer.println(")")
        printer.println()
    }

    @Override
    protected void printContent(PrintStream printer) {}
}
