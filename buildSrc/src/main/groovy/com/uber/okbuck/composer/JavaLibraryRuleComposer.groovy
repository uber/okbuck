package com.uber.okbuck.composer

import com.uber.okbuck.constant.BuckConstants
import com.uber.okbuck.core.model.JavaLibTarget
import com.uber.okbuck.generator.RetroLambdaGenerator
import com.uber.okbuck.rule.JavaLibraryRule
import com.uber.okbuck.printable.PostProcessClassessCommands

final class JavaLibraryRuleComposer extends JavaBuckRuleComposer {

    private JavaLibraryRuleComposer() {
        // no instance
    }

    static JavaLibraryRule compose(JavaLibTarget target, List<String> postProcessCommands) {
        List<String> deps = []
        deps.addAll(external(target.main.externalDeps))
        deps.addAll(targets(target.main.targetDeps))

        Set<String> aptDeps = [] as Set
        aptDeps.addAll(external(target.apt.externalDeps))
        aptDeps.addAll(targets(target.apt.targetDeps))

        Set<String> providedDeps = []
        providedDeps.addAll(external(target.apt.externalDeps))
        providedDeps.addAll(targets(target.apt.targetDeps))
        providedDeps.removeAll(deps)

        PostProcessClassessCommands postprocessClassesCommands = new PostProcessClassessCommands(
                target.bootClasspath,
                target.rootProject.file(BuckConstants.DEFAULT_BUCK_OUT_GEN_PATH).absolutePath);
        if (target.retrolambda) {
            postprocessClassesCommands.addCommand(RetroLambdaGenerator.generate(target))
        }
        postprocessClassesCommands.addCommands(postProcessCommands);

        List<String> testTargets = [];
        if (target.test.sources) {
            testTargets.add(":${test(target)}")
        }

        new JavaLibraryRule(
                src(target),
                ["PUBLIC"],
                deps,
                target.main.sources,
                target.annotationProcessors,
                aptDeps,
                providedDeps,
                target.main.resourcesDir,
                target.sourceCompatibility,
                target.targetCompatibility,
                postprocessClassesCommands,
                target.main.jvmArgs,
                testTargets)
    }

}
