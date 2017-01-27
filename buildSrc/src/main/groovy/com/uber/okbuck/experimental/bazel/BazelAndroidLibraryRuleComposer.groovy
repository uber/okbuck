package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.composer.android.AndroidBuckRuleComposer
import com.uber.okbuck.core.model.android.AndroidLibTarget

final class BazelAndroidLibraryRuleComposer extends AndroidBuckRuleComposer
        implements BazelRuleComposer {

    static BazelAndroidLibraryRule compose(AndroidLibTarget target) {
        List<String> deps = new ArrayList<>();
        Set<String> providedDeps = []

        deps.addAll(external(target.main.externalDeps))
        deps.addAll(targets(target.main.targetDeps))

        providedDeps.addAll(external(target.apt.externalDeps))
        providedDeps.addAll(targets(target.apt.targetDeps))
        providedDeps.removeAll(deps)

        return new BazelAndroidLibraryRule(
                src(target),
                target.getPackage(),
                deps,
                target.main.sources,
                target.manifest,
                providedDeps,
                target.resources)
    }
}
