package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.manager.BuckFileManager;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.NativePrebuilt;
import java.io.File;
import java.util.Collections;
import java.util.List;

public final class D8Util {

  private static final String D8_CACHE = OkBuckGradlePlugin.WORKSPACE_PATH + "/d8";
  private static final String RT_STUB_JAR = "rt-stub.jar";
  public static final String RT_STUB_JAR_RULE = "//" + D8_CACHE + ":" + RT_STUB_JAR;

  private D8Util() {}

  public static void copyDeps(BuckFileManager buckFileManager) {
    FileUtil.copyResourceToProject("d8/" + RT_STUB_JAR, new File(D8_CACHE, RT_STUB_JAR));

    List<Rule> d8 =
        Collections.singletonList(
            new NativePrebuilt()
                .prebuiltType(RuleType.PREBUILT_JAR.getProperties().get(0))
                .prebuilt(RT_STUB_JAR)
                .ruleType(RuleType.PREBUILT_JAR.getBuckName())
                .name(RT_STUB_JAR));

    buckFileManager.writeToBuckFile(d8, new File(D8_CACHE, OkBuckGradlePlugin.BUCK));
  }
}
