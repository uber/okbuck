package com.uber.okbuck.wrapper;

import com.google.common.collect.ImmutableMap;
import com.uber.okbuck.core.util.FileUtil;

import org.apache.commons.io.FilenameUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess", "CanBeFinal", "unused", "ResultOfMethodCallIgnored"})
public class BuckWrapperTask extends DefaultTask {

    private static String OKBUCK_DIRNAME = "            [\"dirname\", \".okbuck\"]";

    @Input
    public String repo;

    @Input
    public Set<String> remove;

    @Input
    public Set<String> keep;

    @Input
    public Set<String> watch;

    @Input
    public Set<String> sourceRoots;

    private final File wrapper = getProject().file("buckw");

    @TaskAction
    void installWrapper() {
        String keepExpr = toWatchmanMatchers(keep);
        if (keepExpr.isEmpty()) {
            keepExpr = OKBUCK_DIRNAME;
        } else {
            keepExpr = OKBUCK_DIRNAME + "\n" + keepExpr;
        }
        Map<String, String> templates = ImmutableMap.<String, String>builder()
                .put("template-creation-time", new Date().toString())
                .put("template-custom-buck-repo", repo)
                .put("template-remove", toWatchmanMatchers(remove))
                .put("template-keep", keepExpr)
                .put("template-watch", toWatchmanMatchers(watch))
                .put("template-source-roots", toWatchmanMatchers(sourceRoots))
                .build();

        FileUtil.copyResourceToProject("wrapper/BUCKW_TEMPLATE", wrapper, templates);
        wrapper.setExecutable(true);

        File watchmanConfig = getProject().file(".watchmanconfig");
        if (!watchmanConfig.exists()) {
            FileUtil.copyResourceToProject("wrapper/WATCHMAN_CONFIG", getProject().file(".watchmanconfig"));
        }
    }

    private static String watchmanExpr(String wildcardPattern) {
        String simplifiedPattern = wildcardPattern;
        if (wildcardPattern.startsWith("**/")) {
            simplifiedPattern = wildcardPattern.replaceAll("\\*\\*/", "");
        }
        String basename = FilenameUtils.getBaseName(simplifiedPattern);
        String extension = FilenameUtils.getExtension(simplifiedPattern);
        if (!simplifiedPattern.contains("/")) {
            // simple file name with no path prefixes
            if (basename.equals("*")) { // suffix
                return "            [\"suffix\", \"" + extension + "\"]";
            } else { // name
                return "            [\"name\", \"" + simplifiedPattern + "\"]";
            }
        }
        return "            [\"match\", \"" + wildcardPattern + "\", \"wholename\"]";
    }

    private static String toWatchmanMatchers(Set<String> wildcardPatterns) {
        return wildcardPatterns
                .parallelStream()
                .map(BuckWrapperTask::watchmanExpr)
                .collect(Collectors.joining(",\n"));
    }
}
