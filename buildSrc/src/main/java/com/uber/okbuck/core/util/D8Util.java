package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;

import java.io.File;

public final class D8Util {

    private static final String D8_CACHE = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/d8";
    private static final String RT_STUB_JAR = "rt-stub.jar";
    public static final String RT_STUB_JAR_RULE = "//" + D8_CACHE + ":" + RT_STUB_JAR;

    private D8Util() {}

    public static void copyDeps() {
        FileUtil.copyResourceToProject("d8/" + RT_STUB_JAR, new File(D8_CACHE, RT_STUB_JAR));
        FileUtil.copyResourceToProject("d8/BUCK_FILE", new File(D8_CACHE, "BUCK"));
    }
}
