package com.github.okbuilds.okbuck.rule

import org.apache.commons.lang.StringUtils

final class AndroidResourceRule extends BuckRule {

    private final String mRes
    private final String mPackage
    private final String mAssets

    AndroidResourceRule(String name, List<String> visibility, List<String> deps, String packageName,
                        String res, String assets) {
        super("android_resource", name, visibility, deps)
        mRes = res
        mPackage = packageName
        mAssets = assets
    }

    @Override
    protected final void printContent(PrintStream printer) {
        if (!StringUtils.isEmpty(mRes)) {
            printer.println("\tres = '${mRes}',")
        }
        printer.println("\tpackage = '${mPackage}',")
        if (!StringUtils.isEmpty(mAssets)) {
            printer.println("\tassets = '${mAssets}',")
        }
        printer.println("\tresource_union = True,")
    }
}
