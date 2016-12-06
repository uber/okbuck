package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.composer.JavaBuckRuleComposer
import com.uber.okbuck.core.model.JavaLibTarget

final class BazelJavaLibraryRuleComposer extends JavaBuckRuleComposer
        implements BazelRuleComposer {
    static BazelJavaLibraryRule compose(JavaLibTarget target) {
        def deps = external(target.main.externalDeps)
        deps += targets(target.main.targetDeps)
        new BazelJavaLibraryRule(src(target), deps, target.main.sources, target.main.resourcesDir)
    }
}
