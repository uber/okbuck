package com.github.okbuilds.core.util

import com.github.okbuilds.core.dependency.DependencyCache
import com.github.okbuilds.core.model.Scope
import com.github.okbuilds.okbuck.OkBuckGradlePlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class RobolectricUtil {

    static final String JSON = 'org.json:json:20080701'
    static final String TAGSOUP = 'org.ccil.cowan.tagsoup:tagsoup:1.2'
    public static final String ROBOLECTRIC_RUNTIME = "robolectricRuntime"

    static enum API {

        API_16('org.robolectric:android-all:4.1.2_r1-robolectric-0', 'org.robolectric:shadows-core:3.0:16'),
        API_17('org.robolectric:android-all:4.2.2_r1.2-robolectric-0', 'org.robolectric:shadows-core:3.0:17'),
        API_18('org.robolectric:android-all:4.3_r2-robolectric-0', 'org.robolectric:shadows-core:3.0:18'),
        API_19('org.robolectric:android-all:4.4_r1-robolectric-1', 'org.robolectric:shadows-core:3.0:19'),
        API_21('org.robolectric:android-all:5.0.0_r2-robolectric-1', 'org.robolectric:shadows-core:3.0:21'),

        private final String androidJar;
        private final String shadowsJar;

        API(String androidJar, String shadowsJar) {
            this.androidJar = androidJar
            this.shadowsJar = shadowsJar
        }
    }

    private RobolectricUtil() {}

    static void download(Project project) {
        List<Configuration> runtimeDeps = []

        Configuration runtimeCommon = project.configurations.maybeCreate("${ROBOLECTRIC_RUNTIME}_common")
        project.dependencies.add(runtimeCommon.name, JSON)
        project.dependencies.add(runtimeCommon.name, TAGSOUP)
        runtimeDeps.add(runtimeCommon)

        EnumSet.allOf(API).each { API api ->
            Configuration runtimeApi = project.configurations.maybeCreate("${ROBOLECTRIC_RUNTIME}_${api.name()}")
            project.dependencies.add(runtimeApi.name, api.androidJar)
            project.dependencies.add(runtimeApi.name, api.shadowsJar)
            runtimeDeps.add(runtimeApi)
        }

        File res = null
        Set<File> sourceDirs = []
        List<String> jvmArguments = []
        runtimeDeps.each { Configuration configuration ->
            DependencyCache cache = new DependencyCache(project, "${OkBuckGradlePlugin.DEFAULT_CACHE_PATH}/robolectric",
                    { File depFile -> depFile.name }, false)
            Scope runtimeDepsScope = new Scope(project, [configuration.name], sourceDirs, res, jvmArguments, cache)
            runtimeDepsScope.getExternalDeps(true)
        }
    }
}
