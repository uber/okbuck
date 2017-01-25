package com.uber.okbuck.composer.base;

import com.uber.okbuck.core.model.base.Target;

import org.apache.commons.io.FilenameUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BuckRuleComposer {

    private static final String SEPARATOR = ":";

    public static Set<String> external(final Set<String> deps) {
        return deps.parallelStream()
                .map(BuckRuleComposer::external)
                .collect(Collectors.toSet());
    }

    public static String external(final String dep) {
        String ext = dep;
        int ind = FilenameUtils.indexOfLastSeparator(dep);
        if (ind >= 0) {
            ext = new StringBuilder(dep).replace(ind, ind + 1, ":").toString();
        }
        return "//" + ext;
    }

    public static Set<String> targets(final Set<Target> deps) {
        return deps.parallelStream()
                .map(BuckRuleComposer::targets)
                .collect(Collectors.toSet());
    }

    public static String targets(final Target dep) {
        return String.format("//%s:src_%s", dep.getPath(), dep.getName());
    }

    public static String binTargets(final Target dep) {
        return String.format("//%s:bin_%s", dep.getPath(), dep.getName());
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
}
