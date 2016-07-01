package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidAppTarget
import com.github.okbuilds.core.model.AndroidLibTarget
import com.github.okbuilds.core.model.JavaTarget
import com.github.okbuilds.okbuck.rule.ProjectConfigRule

import static com.github.okbuilds.okbuck.composer.AndroidBuckRuleComposer.bin

class ProjectConfigComposer extends JavaBuckRuleComposer {

    private ProjectConfigComposer() {
        // no instance
    }

    static ProjectConfigRule composeAndroidApp(AndroidAppTarget androidAppTarget) {
        return compose(bin(androidAppTarget), null, androidAppTarget)
    }

    static ProjectConfigRule composeAndroidLibrary(AndroidLibTarget androidLibTarget) {
      return compose(src(androidLibTarget), null, androidLibTarget)
    }

    static ProjectConfigRule composeJavaLibrary(JavaTarget javaTarget) {
        return compose(src(javaTarget), test(javaTarget), javaTarget)
    }

    private static ProjectConfigRule compose(String targetName, String testTargetName, JavaTarget target) {
        Set<String> mainSources = new LinkedHashSet<>()
        Set<String> testSources = new LinkedHashSet<>()
        mainSources.addAll(target.main.sources)
        testSources.addAll(target.test.sources)

        return new ProjectConfigRule(targetName, mainSources, testTargetName, testSources)
    }
}
