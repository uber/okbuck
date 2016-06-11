package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidTarget
import com.github.okbuilds.okbuck.rule.PrebuiltNativeLibraryRule

final class PreBuiltNativeLibraryRuleComposer {

    private PreBuiltNativeLibraryRuleComposer() {
        // no instance
    }

    static PrebuiltNativeLibraryRule compose(AndroidTarget target, String jniLibDir) {
        String ruleName = "prebuilt_native_library_${target.name}_${jniLibDir.replaceAll("/", "_")}"
        return new PrebuiltNativeLibraryRule(ruleName, Arrays.asList("PUBLIC"), jniLibDir)
    }
}
