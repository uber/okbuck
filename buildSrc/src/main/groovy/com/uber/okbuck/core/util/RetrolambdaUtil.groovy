package com.uber.okbuck.core.util

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.extension.RetrolambdaExtension
import org.gradle.api.Project

class RetrolambdaUtil {

    static final String RETROLAMBDA_DEPS_CONFIG = "okbuckRetrolambda"
    static final String RETROLAMBDA_GROUP = "net.orfjackal.retrolambda"
    static final String RETROLAMBDA_MODULE = "retrolambda"
    static final String RETROLAMDBA_CACHE = "${OkBuckGradlePlugin.DEFAULT_CACHE_PATH}/retrolambda"
    static final String RT_STUB_JAR = "rt-stub.jar"
    static final String RETROLAMBDA_DEPS_BUCK_FILE = "retrolambda/BUCK_FILE"

    private static String sRetrolambdaCmd

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
        Scope retrolambdaDepsScope = new Scope(
                project,
                [RETROLAMBDA_DEPS_CONFIG],
                sourceDirs,
                res,
                jvmArguments,
                retrolambdaDepCache)

        String retrolambdaJar = retrolambdaDepsScope.getExternalDeps()[0]

        FileUtil.copyResourceToProject(RETROLAMBDA_DEPS_BUCK_FILE, new File(retrolambdaDepCache.cacheDir, "BUCK"))
        FileUtil.copyResourceToProject("retrolambda/${RT_STUB_JAR}", new File(retrolambdaDepCache.cacheDir, RT_STUB_JAR))

        sRetrolambdaCmd = "(read CLASSES_DIR && java -Dretrolambda.inputDir=\$CLASSES_DIR " +
                "-Dretrolambda.classpath=\"\${COMPILATION_BOOTCLASSPATH}\":\"\${COMPILATION_CLASSPATH}\":\"\${CLASSES_DIR}\"" +
                "${extension.jvmArgs} -jar ${retrolambdaJar}) <<<"
    }

    static String getRetrolambdaCmd() {
        return sRetrolambdaCmd
    }

    static DependencyCache getRetrolambdaDepsCache(Project project) {
        return new DependencyCache("retrolambda",
                project.rootProject,
                RETROLAMDBA_CACHE,
                [project.configurations.getByName(RETROLAMBDA_DEPS_CONFIG)] as Set,
                RETROLAMBDA_DEPS_BUCK_FILE)
    }
}
