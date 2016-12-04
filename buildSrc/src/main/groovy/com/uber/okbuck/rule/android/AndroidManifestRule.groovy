package com.uber.okbuck.rule.android

import com.uber.okbuck.rule.base.BuckRule

final class AndroidManifestRule extends BuckRule {

    private final String mSkeleton

    AndroidManifestRule(String name, List<String> visibility, List<String> deps, String skeleton) {
        super("android_manifest", name, visibility, deps)
        mSkeleton = skeleton
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tskeleton = '${mSkeleton}',")
    }
}
