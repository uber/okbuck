package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.util.RetrolambdaUtil
import com.uber.okbuck.rule.android.KotlinAndroidLibraryRule

class KotlinAndroidLibraryRuleComposer extends AndroidBuckRuleComposer {

  private KotlinAndroidLibraryRuleComposer() {}

  static KotlinAndroidLibraryRule compose(
      AndroidLibTarget target,
      List<String> deps,
      final List<String> aidlRuleNames,
      String appClass) {
    List<String> libraryDeps = new ArrayList<>(deps)
    libraryDeps.addAll(external(target.main.externalDeps))
    libraryDeps.addAll(targets(target.main.targetDeps))

    List<String> libraryAptDeps = []
    libraryAptDeps.addAll(externalApt(target.apt.externalDeps))
    libraryAptDeps.addAll(targetsApt(target.apt.targetDeps))

    Set<String> providedDeps = []
    providedDeps.addAll(external(target.provided.externalDeps))
    providedDeps.addAll(targets(target.provided.targetDeps))
    providedDeps.removeAll(libraryDeps)

    if (target.retrolambda) {
      providedDeps.add(RetrolambdaUtil.getRtStubJarRule())
    }

    libraryDeps.addAll(target.main.targetDeps.findAll { Target targetDep ->
      targetDep instanceof AndroidTarget
    }.collect { Target targetDep ->
      resRule(targetDep as AndroidTarget)
    })

    List<String> testTargets = []
    if (target.robolectric && target.test.sources) {
      testTargets.add(":${test(target)}")
    }

    return new KotlinAndroidLibraryRule(
        src(target),
        ["PUBLIC"],
        libraryDeps,
        target.main.sources,
        fileRule(target.manifest),
        target.annotationProcessors as List,
        libraryAptDeps,
        providedDeps,
        aidlRuleNames,
        appClass,
        target.sourceCompatibility,
        target.targetCompatibility,
        target.postprocessClassesCommands,
        target.main.jvmArgs,
        target.generateR2,
        testTargets,
        target.getExtraOpts(RuleType.KOTLIN_ANDROID_LIBRARY))
  }
}
