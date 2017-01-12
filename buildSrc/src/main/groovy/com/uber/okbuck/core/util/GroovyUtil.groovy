package com.uber.okbuck.core.util

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class GroovyUtil {

    static final String GROOVY_DEPS_CONFIG = "okbuck_groovy_deps"
    static final String GROOVY_HOME_LOCATION = "${OkBuckGradlePlugin.DEFAULT_CACHE_PATH}/groovy_installation"

    private GroovyUtil() {}

    static void setupGroovyHome(Project rootProject) {
        String groovyVersion = GroovySystem.getVersion()

        Configuration groovyConfig = rootProject.configurations.maybeCreate(GROOVY_DEPS_CONFIG)
        rootProject.dependencies {
            "${GROOVY_DEPS_CONFIG}" "org.codehaus.groovy:groovy:${groovyVersion}"
        }
        new DependencyCache("groovy" as String,
                rootProject,
                "${GROOVY_HOME_LOCATION}/lib" as String,
                [groovyConfig] as Set,
                null)

        File groovyStarterConf = new File("${GROOVY_HOME_LOCATION}/conf/groovy-starter.conf")
        groovyStarterConf.parentFile.mkdirs()
        FileUtil.copyResourceToProject("groovy/conf/groovy-starter.conf", groovyStarterConf)

        File groovyc = new File("${GROOVY_HOME_LOCATION}/bin/groovyc")
        groovyc.parentFile.mkdirs()
        FileUtil.copyResourceToProject("groovy/bin/groovyc", new File("${GROOVY_HOME_LOCATION}/bin/groovyc"))
        groovyc.text = groovyc.text.replaceFirst("template-groovy-version", groovyVersion)
        groovyc.setExecutable(true)

        File startGroovy = new File("${GROOVY_HOME_LOCATION}/bin/startGroovy")
        FileUtil.copyResourceToProject("groovy/bin/startGroovy", new File("${GROOVY_HOME_LOCATION}/bin/startGroovy"))
        startGroovy.text = startGroovy.text.replaceFirst("template-groovy-version", groovyVersion)
        startGroovy.setExecutable(true)
    }
}
