package com.uber.okbuck.core.manager;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.manager.BuckFileManager;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.NativePrebuilt;
import com.uber.okbuck.core.util.FileUtil;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.gradle.api.Project;

public final class D8Manager {

  private static final String D8_CACHE = OkBuckGradlePlugin.WORKSPACE_PATH + "/d8";
  private static final String RT_STUB_JAR = "rt-stub.jar";
  public static final String RT_STUB_JAR_RULE = "//" + D8_CACHE + ":" + RT_STUB_JAR;

  private final Project rootProject;

  public D8Manager(Project rootProject) {
    this.rootProject = rootProject;
  }

  public void copyDeps(BuckFileManager buckFileManager, OkBuckExtension okBuckExtension) {
    FileUtil.copyResourceToProject("d8/" + RT_STUB_JAR, rootProject.file(D8_CACHE + File.separator + RT_STUB_JAR));

    List<Rule> d8 =
        Collections.singletonList(
            new NativePrebuilt()
                .prebuiltType(RuleType.PREBUILT_JAR.getProperties().get(0))
                .prebuilt(RT_STUB_JAR)
                .ruleType(RuleType.PREBUILT_JAR.getBuckName())
                .name(RT_STUB_JAR));

    buckFileManager.writeToBuckFile(d8, rootProject.file(D8_CACHE + File.separator + okBuckExtension.buildFileName));
  }
}
