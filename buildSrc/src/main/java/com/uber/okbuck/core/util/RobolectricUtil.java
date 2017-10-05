package com.uber.okbuck.core.util;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.DependencyUtils;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.util.EnumSet;

public final class RobolectricUtil {

    private static final String JSON = "org.json:json:20080701";
    private static final String TAGSOUP = "org.ccil.cowan.tagsoup:tagsoup:1.2";
    private static final String ROBOLECTRIC_RUNTIME = "robolectricRuntime";
    public static final String ROBOLECTRIC_CACHE = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/robolectric";

    private RobolectricUtil() {}

    public static void download(Project project) {
        ImmutableSet.Builder<Configuration> runtimeDeps = ImmutableSet.builder();

        Configuration runtimeCommon = project.getConfigurations().maybeCreate(ROBOLECTRIC_RUNTIME + "_common");
        project.getDependencies().add(runtimeCommon.getName(), JSON);
        project.getDependencies().add(runtimeCommon.getName(), TAGSOUP);
        runtimeDeps.add(runtimeCommon);

        for (API api : EnumSet.allOf(API.class)) {
            Configuration runtimeApi = project.getConfigurations().maybeCreate(
                    ROBOLECTRIC_RUNTIME + "_" + api.name());
            project.getDependencies().add(runtimeApi.getName(), api.androidJar);
            if (api.shadowsJar != null) {
                project.getDependencies().add(runtimeApi.getName(), api.shadowsJar);
            }
            runtimeDeps.add(runtimeApi);
        }

        DependencyCache dependencyCache = new DependencyCache(project,
                DependencyUtils.createCacheDir(project, ROBOLECTRIC_CACHE));
        for (Configuration configuration : runtimeDeps.build()) {
            dependencyCache.build(configuration, false);
        }
        dependencyCache.cleanup();
    }

    @SuppressWarnings("unused")
    enum API {
        API_16("org.robolectric:android-all:4.1.2_r1-robolectric-0", "org.robolectric:shadows-core:3.0:16"),
        API_17("org.robolectric:android-all:4.2.2_r1.2-robolectric-0", "org.robolectric:shadows-core:3.0:17"),
        API_18("org.robolectric:android-all:4.3_r2-robolectric-0", "org.robolectric:shadows-core:3.0:18"),
        API_19("org.robolectric:android-all:4.4_r1-robolectric-1", "org.robolectric:shadows-core:3.0:19"),
        API_21("org.robolectric:android-all:5.0.0_r2-robolectric-1", "org.robolectric:shadows-core:3.0:21"),
        API_22("org.robolectric:android-all:5.1.1_r9-robolectric-1", null),
        API_23("org.robolectric:android-all:6.0.1_r3-robolectric-0", null),
        API_24("org.robolectric:android-all:7.0.0_r1-robolectric-0", null),
        API_25("org.robolectric:android-all:7.1.0_r7-robolectric-0", null),
        API_26("org.robolectric:android-all:8.0.0_r4-robolectric-0", null);

        private final String androidJar;
        private final String shadowsJar;

        API(String androidJar, String shadowsJar) {
            this.androidJar = androidJar;
            this.shadowsJar = shadowsJar;
        }
    }
}
