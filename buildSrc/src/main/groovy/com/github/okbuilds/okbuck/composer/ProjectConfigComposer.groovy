package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidAppTarget
import com.github.okbuilds.core.model.AndroidLibTarget
import com.github.okbuilds.core.model.AndroidTarget
import com.github.okbuilds.core.model.JavaLibTarget
import com.github.okbuilds.core.model.JavaTarget
import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.rule.ProjectConfigRule

import static com.github.okbuilds.okbuck.composer.AndroidBuckRuleComposer.bin

class ProjectConfigComposer extends JavaBuckRuleComposer {

    private ProjectConfigComposer() {
        // no instance
    }

    static ProjectConfigRule composeAndroidApp(AndroidAppTarget androidAppTarget) {
        return compose(bin(androidAppTarget), androidAppTarget)
    }

    static ProjectConfigRule composeLibrary(JavaTarget javaTarget) {
        return compose(src(javaTarget), javaTarget)
    }

    private static ProjectConfigRule compose(String targetName, JavaTarget target) {
        Set<String> mainSources = new LinkedHashSet<>()
        Set<String> testSources = new LinkedHashSet<>()
        mainSources.addAll(target.main.sources)
        testSources.addAll(target.test.sources)

        return new ProjectConfigRule(targetName, mainSources, null, testSources)
    }
}
