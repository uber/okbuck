package com.uber.okbuck.composer.base;

import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.model.java.JavaLibTarget;
import com.uber.okbuck.core.util.ProjectUtil;

import org.apache.commons.io.FilenameUtils;
import org.gradle.api.Project;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BuckRuleComposer {

    private static final String SEPARATOR = ":";

    public static Set<String> external(final Set<String> deps, final Target target) {
        return deps.parallelStream()
                .map(dep -> fileRule(dep, target))
                .collect(Collectors.toSet());
    }

    public static Set<String> externalApt(final Set<String> deps, final Target target) {
        return external(deps, target).parallelStream()
                .filter(dep -> dep.endsWith(".jar"))
                .collect(Collectors.toSet());
    }

    public static String fileRule(final String filePath, final Target target) {
        if (filePath == null) {
            return null;
        }

        StringBuilder ext = new StringBuilder(filePath);
        int ind = FilenameUtils.indexOfLastSeparator(filePath);
        if (ind >= 0) {
            ext = ext.replace(ind, ind + 1, ":");
        }

        return ext.insert(0, rootPrefix(target)).toString();
    }

    public static Set<String> targets(final Set<Target> deps) {
        return deps.parallelStream()
                .map(BuckRuleComposer::targets)
                .collect(Collectors.toSet());
    }

    public static Set<String> targetsApt(final Set<Target> deps) {
        return deps.parallelStream()
                .filter(target -> target.getClass().equals(JavaLibTarget.class))
                .map(BuckRuleComposer::targets)
                .collect(Collectors.toSet());
    }

    public static String targets(final Target dep) {
        return String.format("%s%s:src_%s", rootPrefix(dep), dep.getPath(), dep.getName());
    }

    public static String binTargets(final Target dep) {
        return String.format("%s%s:bin_%s", rootPrefix(dep), dep.getPath(), dep.getName());
    }

    public static String toLocation(final List<String> targets) {
        return targets.parallelStream()
                .map(BuckRuleComposer::toLocation)
                .collect(Collectors.joining(SEPARATOR));
    }

    public static String toLocation(final String target) {
        return "$(location " + target + ")";
    }

    public static String toClasspath(final String target) {
        return "$(classpath " + target + ")";
    }

    public static String rootPrefix(final Target target) {
        return "//" + relativeRoot(target.getRootProject());
    }

    public static String relativeRoot(Project project) {
        File projectRoot = project.getRootDir();
        File buckRoot = (File) ProjectUtil.getExtProperty(project, "buckRootDirectory");

        if (projectRoot.equals(buckRoot)) {
            return "";
        } else if (!isParent(buckRoot, projectRoot)) {
            throw new IllegalStateException(
                    "The buck root dir must be the same dir or parent of the okbuck project/target dir.");
        }

        return buckRoot.toURI().relativize(projectRoot.toURI()).toString();
    }

    private static boolean isParent(File parent, File possibleChild) {
        return possibleChild.getAbsolutePath().startsWith(parent.getAbsolutePath());
    }
}
