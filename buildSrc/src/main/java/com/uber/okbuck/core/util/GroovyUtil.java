package com.uber.okbuck.core.util;

import com.google.common.collect.ImmutableMap;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.io.File;
import java.util.Collections;

import groovy.lang.GroovySystem;

public final class GroovyUtil {

    private static final String GROOVY_DEPS_CONFIG = "okbuck_groovy_deps";

    public static final String GROOVY_HOME_LOCATION = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/groovy_installation";

    private GroovyUtil() {}

    public static void setupGroovyHome(Project rootProject) {
        String groovyVersion = GroovySystem.getVersion();

        Configuration groovyConfig = rootProject.getConfigurations().maybeCreate(GROOVY_DEPS_CONFIG);
        rootProject.getDependencies().add(GROOVY_DEPS_CONFIG, "org.codehaus.groovy:groovy:" + groovyVersion);
        new DependencyCache("groovy",
                rootProject,
                GROOVY_HOME_LOCATION + "/lib",
                Collections.singleton(groovyConfig),
                null);

        File groovyHome = new File(GROOVY_HOME_LOCATION);

        File groovyStarterConf = new File(groovyHome, "conf/groovy-starter.conf");
        FileUtil.copyResourceToProject("groovy/conf/groovy-starter.conf", groovyStarterConf);

        File groovyc = new File(groovyHome, "bin/groovyc");
        FileUtil.copyResourceToProject("groovy/bin/groovyc",
                new File(groovyHome, "bin/groovyc"),
                ImmutableMap.of("template-groovy-version", groovyVersion));
        groovyc.setExecutable(true);

        File startGroovy = new File(groovyHome, "bin/startGroovy");
        FileUtil.copyResourceToProject("groovy/bin/startGroovy",
                new File(groovyHome, "/bin/startGroovy"),
                ImmutableMap.of("template-groovy-version", groovyVersion));
        startGroovy.setExecutable(true);
    }
}
