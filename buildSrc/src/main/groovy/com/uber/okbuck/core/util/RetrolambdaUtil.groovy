package com.uber.okbuck.core.util

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.model.Scope
import com.uber.okbuck.extension.RetrolambdaExtension
import org.gradle.api.Project

import java.nio.file.Files

class RetrolambdaUtil {

    static final String RETROLAMBDA_DEPS_CONFIG = "okbuckRetrolambda"
    static final String RETROLAMBDA_GROUP = "net.orfjackal.retrolambda"
    static final String RETROLAMBDA_MODULE = "retrolambda"
    static final String RETROLAMDBA_CACHE = "${OkBuckGradlePlugin.DEFAULT_CACHE_PATH}/retrolambda"
    static final String RT_STUB_JAR = "rt-stub.jar"
    static final String RETROLAMBDA_DEPS_BUCK_FILE = "retrolambda/BUCK_FILE"
    static final String RETROLAMBDAC = "retrolambdac"
    static final String RETROLAMBDA_BUILD_DIR = "build/okbuck/retrolambda"
    static final String PROJECT_RETROLAMBDAC = "${RETROLAMBDA_BUILD_DIR}/${RETROLAMBDAC}"

    private RetrolambdaUtil() {}

    static String getRtStubJarRule() {
        return "//${RETROLAMDBA_CACHE}:${RT_STUB_JAR}"
    }

    static void fetchRetrolambdaDeps(Project project, RetrolambdaExtension extension) {
        if (!extension.version) {
            throw new IllegalStateException("Invalid retrolambda version: ${extension.version}")
        }

        project.configurations.maybeCreate(RETROLAMBDA_DEPS_CONFIG)
        project.dependencies {
            "${RETROLAMBDA_DEPS_CONFIG}" "${RETROLAMBDA_GROUP}:${RETROLAMBDA_MODULE}:${extension.version}"
        }

        File res = null
        Set<File> sourceDirs = []
        List<String> jvmArguments = []
        DependencyCache retrolambdaDepCache = getRetrolambdaDepsCache(project)
        Scope retrolambdaDepsScope = new Scope(project, [RETROLAMBDA_DEPS_CONFIG], sourceDirs, res, jvmArguments,
                retrolambdaDepCache)
        String retrolambdaJar = retrolambdaDepsScope.getExternalDeps()[0]

        FileUtil.copyResourceToProject(RETROLAMBDA_DEPS_BUCK_FILE, new File(retrolambdaDepCache.cacheDir, "BUCK"))
        FileUtil.copyResourceToProject("retrolambda/${RT_STUB_JAR}", new File(retrolambdaDepCache.cacheDir, RT_STUB_JAR))

        File retrolambdac = new File(retrolambdaDepCache.cacheDir, RETROLAMBDAC)
        FileUtil.copyResourceToProject("retrolambda/${RETROLAMBDAC}", retrolambdac)
        retrolambdac.text = retrolambdac.text
                .replaceFirst("template-retrolambda-jvm-args", extension.jvmArgs)
                .replaceFirst("template-retrolambda-jar", retrolambdaJar.replaceFirst("${RETROLAMDBA_CACHE}/", ""))
        retrolambdac.setExecutable(true)
    }

    static void createRetrolambdac(Project project) {
        File retrolambdac = project.file(PROJECT_RETROLAMBDAC)
        if (!retrolambdac.exists()) {
            retrolambdac.parentFile.mkdirs()
            Files.createSymbolicLink(retrolambdac.toPath(),
                    project.rootProject.file("${RETROLAMDBA_CACHE}/${RETROLAMBDAC}").toPath())
        }
    }

    static DependencyCache getRetrolambdaDepsCache(Project project) {
        return new DependencyCache(project.rootProject, RETROLAMDBA_CACHE, false, RETROLAMBDA_DEPS_BUCK_FILE) {

            @Override
            boolean isValid(File dep) {
                return dep.name.endsWith(".jar")
            }
        }
    }
}
