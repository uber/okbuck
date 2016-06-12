package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidTarget
import com.github.okbuilds.okbuck.rule.PrebuiltNativeLibraryRule

final class PreBuiltNativeLibraryRuleComposer extends AndroidBuckRuleComposer {

    private PreBuiltNativeLibraryRuleComposer() {
        // no instance
    }

    static PrebuiltNativeLibraryRule compose(AndroidTarget target, String jniLibDir) {
        return new PrebuiltNativeLibraryRule(prebuiltNative(target, jniLibDir),
                Arrays.asList("PUBLIC"), jniLibDir)
    }
}
