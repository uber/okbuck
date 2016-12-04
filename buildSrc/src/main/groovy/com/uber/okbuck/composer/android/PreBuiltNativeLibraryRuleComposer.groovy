package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.rule.android.PrebuiltNativeLibraryRule

final class PreBuiltNativeLibraryRuleComposer extends AndroidBuckRuleComposer {

    private PreBuiltNativeLibraryRuleComposer() {
        // no instance
    }

    static PrebuiltNativeLibraryRule compose(AndroidTarget target, String jniLibDir) {
        return new PrebuiltNativeLibraryRule(prebuiltNative(target, jniLibDir),
                Arrays.asList("PUBLIC"), jniLibDir)
    }
}
