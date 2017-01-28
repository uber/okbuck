package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public final class RobolectricUtil {

    private static final String JSON = "org.json:json:20080701";
    private static final String TAGSOUP = "org.ccil.cowan.tagsoup:tagsoup:1.2";
    private static final String ROBOLECTRIC_RUNTIME = "robolectricRuntime";
    public static final String ROBOLECTRIC_CACHE = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/robolectric";

    private RobolectricUtil() {}

    public static void download(Project project) {
        List<Configuration> runtimeDeps = new ArrayList<>();

        Configuration runtimeCommon = project.getConfigurations().maybeCreate(ROBOLECTRIC_RUNTIME + "_common");
        project.getDependencies().add(runtimeCommon.getName(), JSON);
        project.getDependencies().add(runtimeCommon.getName(), TAGSOUP);
        runtimeDeps.add(runtimeCommon);

        for (API api : EnumSet.allOf(API.class)) {
            Configuration runtimeApi = project.getConfigurations().maybeCreate(
                    ROBOLECTRIC_RUNTIME + "_" + api.name());
            project.getDependencies().add(runtimeApi.getName(), api.androidJar);
            project.getDependencies().add(runtimeApi.getName(), api.shadowsJar);
            runtimeDeps.add(runtimeApi);
        }

        for (Configuration configuration : runtimeDeps) {
            new DependencyCache("robolectric" + configuration.getName().toUpperCase(),
                    project,
                    ROBOLECTRIC_CACHE,
                    Collections.singleton(configuration),
                    null,
                    false);
        }
    }

    @SuppressWarnings("unused")
    enum API {
        API_16("org.robolectric:android-all:4.1.2_r1-robolectric-0", "org.robolectric:shadows-core:3.0:16"),
        API_17("org.robolectric:android-all:4.2.2_r1.2-robolectric-0", "org.robolectric:shadows-core:3.0:17"),
        API_18("org.robolectric:android-all:4.3_r2-robolectric-0", "org.robolectric:shadows-core:3.0:18"),
        API_19("org.robolectric:android-all:4.4_r1-robolectric-1", "org.robolectric:shadows-core:3.0:19"),
        API_21("org.robolectric:android-all:5.0.0_r2-robolectric-1", "org.robolectric:shadows-core:3.0:21");

        private final String androidJar;
        private final String shadowsJar;

        API(String androidJar, String shadowsJar) {
            this.androidJar = androidJar;
            this.shadowsJar = shadowsJar;
        }
    }
}
