package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.template.config.Groovyc;
import com.uber.okbuck.template.config.StartGroovy;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.io.File;

import groovy.lang.GroovySystem;

public final class GroovyUtil {

    private static final String GROOVY_DEPS_CONFIG = "okbuck_groovy_deps";

    public static final String GROOVY_HOME_LOCATION = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/groovy_installation";

    private GroovyUtil() {}

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void setupGroovyHome(Project project) {
        String groovyVersion = GroovySystem.getVersion();

        Configuration groovyConfig = project.getConfigurations().maybeCreate(GROOVY_DEPS_CONFIG);
        project.getDependencies().add(GROOVY_DEPS_CONFIG, "org.codehaus.groovy:groovy:" + groovyVersion);
        new DependencyCache(project, DependencyUtils.createCacheDir(project, GROOVY_HOME_LOCATION + "/lib"))
                .build(groovyConfig);

        File groovyHome = project.file(GROOVY_HOME_LOCATION);

        File groovyStarterConf = new File(groovyHome, "conf/groovy-starter.conf");
        FileUtil.copyResourceToProject("groovy/conf/groovy-starter.conf", groovyStarterConf);

        File groovyc = new File(groovyHome, "bin/groovyc");
        new Groovyc().groovyVersion(groovyVersion).render(groovyc);
        groovyc.setExecutable(true);

        File startGroovy = new File(groovyHome, "bin/startGroovy");
        new StartGroovy().groovyVersion(groovyVersion).render(startGroovy);
        startGroovy.setExecutable(true);
    }
}
