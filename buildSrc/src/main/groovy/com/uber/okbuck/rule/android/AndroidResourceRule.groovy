package com.uber.okbuck.rule.android

import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.rule.base.BuckRule

final class AndroidResourceRule extends BuckRule {

    private final String mPackage
    private final Set<String> mRes
    private final Set<String> mAssets
    private final boolean mResourceUnion

    AndroidResourceRule(String name, List<String> visibility, List<String> deps, String packageName,
                        Set<String> res, Set<String> assets, boolean resourceUnion) {
        super(RuleType.ANDROID_RESOURCE, name, visibility, deps)
        mRes = res
        mPackage = packageName
        mAssets = assets
        mResourceUnion = resourceUnion
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tpackage = '${mPackage}',")
        if (mRes || mResourceUnion) {
            printer.println("\tres = res_glob([")
            mRes.each {
                printer.println("\t\t('${it}', '**'),")
            }
            printer.println("\t]),")
        }
        if (mAssets) {
            printer.println("\tassets = subdir_glob([")
            mAssets.each {
                printer.println("\t\t('${it}', '**'),")
            }
            printer.println("\t]),")
        }
        if (mResourceUnion) {
            printer.println("\tresource_union = True,")
        }
    }
}
