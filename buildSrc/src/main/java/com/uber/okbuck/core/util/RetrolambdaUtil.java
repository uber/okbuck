package com.uber.okbuck.core.util;

import com.google.common.collect.ImmutableList;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.extension.RetrolambdaExtension;

import org.apache.commons.lang.StringUtils;
import org.gradle.api.Project;

import java.io.File;
import java.util.Collections;

public final class RetrolambdaUtil {

    private static final String RETROLAMBDA_DEPS_CONFIG = "okbuckRetrolambda";
    private static final String RETROLAMBDA_GROUP = "net.orfjackal.retrolambda";
    private static final String RETROLAMBDA_MODULE = "retrolambda";
    private static final String RETROLAMDBA_CACHE = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/retrolambda";
    private static final String RT_STUB_JAR = "rt-stub.jar";
    private static final String RETROLAMBDA_DEPS_BUCK_FILE = "retrolambda/BUCK_FILE";

    private static final String RETROLAMBDA_CMD_TEMPLATE =
            "(read CLASSES_DIR && java -Dretrolambda.inputDir=\"${CLASSES_DIR}\" " +
                    "-Dretrolambda.classpath=\"${COMPILATION_BOOTCLASSPATH}\":"
                    + "\"${COMPILATION_CLASSPATH}\":"
                    + "\"${CLASSES_DIR}\"";

    private RetrolambdaUtil() {}

    public static String getRtStubJarRule() {
        return "//" + RETROLAMDBA_CACHE + ":" + RT_STUB_JAR;
    }

    public static void fetchRetrolambdaDeps(Project project, RetrolambdaExtension extension) {
        if (extension.getVersion() == null) {
            throw new IllegalStateException("Invalid retrolambda version");
        }

        project.getConfigurations().maybeCreate(RETROLAMBDA_DEPS_CONFIG);
        project.getDependencies().add(RETROLAMBDA_DEPS_CONFIG,
                RETROLAMBDA_GROUP + ":" + RETROLAMBDA_MODULE + ":" + extension.getVersion()
        );

        DependencyCache retrolambdaDepCache = getRetrolambdaDepsCache(project);
        Scope retrolambdaDepsScope = new Scope(
                project,
                Collections.singleton(RETROLAMBDA_DEPS_CONFIG),
                Collections.emptySet(),
                null,
                Collections.emptyList(),
                retrolambdaDepCache);

        String retrolambdaJar = retrolambdaDepsScope.getExternalDeps().iterator().next();

        FileUtil.copyResourceToProject(RETROLAMBDA_DEPS_BUCK_FILE,
                new File(retrolambdaDepCache.getCacheDir(), "BUCK"));
        FileUtil.copyResourceToProject("retrolambda/" + RT_STUB_JAR,
                new File(retrolambdaDepCache.getCacheDir(), RT_STUB_JAR));

        ImmutableList.Builder<String> builder = ImmutableList.<String>builder().add(RETROLAMBDA_CMD_TEMPLATE);
        if (!StringUtils.isEmpty(extension.getJvmArgs())) {
            builder = builder.add(extension.getJvmArgs());
        }
        builder = builder.add("-jar").add(retrolambdaJar + ")").add("<<<");
        ProjectUtil.getPlugin(project).retrolambdaCmd = String.join(" ", builder.build());
    }

    public static String getRetrolambdaCmd(Project project) {
        return ProjectUtil.getPlugin(project).retrolambdaCmd;
    }

    private static DependencyCache getRetrolambdaDepsCache(Project project) {
        return new DependencyCache("retrolambda",
                project.getRootProject(),
                RETROLAMDBA_CACHE,
                Collections.singleton(project.getConfigurations().getByName(RETROLAMBDA_DEPS_CONFIG)),
                RETROLAMBDA_DEPS_BUCK_FILE);
    }
}
