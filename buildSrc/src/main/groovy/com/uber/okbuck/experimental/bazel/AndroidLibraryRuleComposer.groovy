package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.composer.AndroidBuckRuleComposer
import com.uber.okbuck.core.model.AndroidLibTarget

final class AndroidLibraryRuleComposer extends AndroidBuckRuleComposer {

    static AndroidLibraryRule compose(AndroidLibTarget target) {
        List<String> deps = new ArrayList<>();
        Set<String> providedDeps = []

        deps.addAll(OkBazelGradlePlugin.external(target.main.externalDeps))
        deps.addAll(targets(target.main.targetDeps))

        providedDeps.addAll(OkBazelGradlePlugin.external(target.apt.externalDeps))
        providedDeps.addAll(targets(target.apt.targetDeps))
        providedDeps.removeAll(deps)

        return new AndroidLibraryRule(
                src(target),
                target.getPackage(),
                deps,
                target.main.sources,
                target.manifest,
                providedDeps,
                target.resources)
    }
}
