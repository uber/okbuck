package com.uber.okbuck.composer

import com.uber.okbuck.core.model.AndroidTarget
import com.uber.okbuck.rule.PrebuiltNativeLibraryRule

final class PreBuiltNativeLibraryRuleComposer extends AndroidBuckRuleComposer {

    private PreBuiltNativeLibraryRuleComposer() {
        // no instance
    }

    static PrebuiltNativeLibraryRule compose(AndroidTarget target, String jniLibDir) {
        return new PrebuiltNativeLibraryRule(prebuiltNative(target, jniLibDir),
                Arrays.asList("PUBLIC"), jniLibDir)
    }
}
