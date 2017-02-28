package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.util.RetrolambdaUtil
import com.uber.okbuck.core.util.RobolectricUtil
import com.uber.okbuck.rule.android.AndroidTestRule

final class AndroidTestRuleComposer extends AndroidBuckRuleComposer {

    private AndroidTestRuleComposer() {
        // no instance
    }

    static AndroidTestRule compose(
            AndroidLibTarget target,
            List<String> deps,
            final List<String> aidlRuleNames,
            String appClass) {

        List<String> testDeps = new ArrayList<>(deps)
        testDeps.add(":${src(target)}")
        testDeps.addAll(external(target.test.externalDeps))
        testDeps.addAll(targets(target.test.targetDeps))

        List<String> testAptDeps = []
        testAptDeps.addAll(external(target.testApt.externalDeps))
        testAptDeps.addAll(targets(target.testApt.targetDeps))

        Set<String> providedDeps = []
        providedDeps.addAll(external(target.testProvided.externalDeps))
        providedDeps.addAll(targets(target.testProvided.targetDeps))
        providedDeps.removeAll(testDeps)

        if (target.retrolambda) {
            providedDeps.add(RetrolambdaUtil.getRtStubJarRule())
        }

        return new AndroidTestRule(
                test(target),
                ["PUBLIC"],
                testDeps,
                target.test.sources,
                target.manifest,
                target.testAnnotationProcessors as List,
                testAptDeps,
                providedDeps,
                aidlRuleNames,
                appClass,
                target.sourceCompatibility,
                target.targetCompatibility,
                target.postprocessClassesCommands,
                target.test.jvmArgs,
                target.testOptions,
                target.test.resourcesDir,
                RobolectricUtil.ROBOLECTRIC_CACHE,
                target.getExtraOpts(RuleType.ROBOLECTRIC_TEST))
    }
}
