package com.uber.okbuck.composer.jvm;

import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.model.java.JavaTarget;
import com.uber.okbuck.core.model.jvm.JvmTarget;

import java.util.HashSet;
import java.util.Set;

public class JvmBuckRuleComposer extends BuckRuleComposer {

    public static String src(final JvmTarget target) {
        return "src_" + target.getName();
    }

    public static String bin(final JvmTarget target) {
        return "bin_" + target.getName();
    }

    public static String test(final JvmTarget target) {
        return "test_" + target.getName();
    }

    public static Set<Target> getTargetDeps(final Scope main, final Scope provided) {
        Set<Target> runtimeOnlyDeps = new HashSet<>(main.getTargetDeps());
        runtimeOnlyDeps.removeAll(provided.getTargetDeps());

        Set<Target> deps = new HashSet<>(main.getTargetDeps());
        deps.removeAll(runtimeOnlyDeps);
        return deps;
    }

    public static Set<Target> getTargetProvidedDeps(final Scope main, final Scope provided) {
        Set<Target> deps = new HashSet<>(provided.getTargetDeps());
        deps.removeAll(main.getTargetDeps());
        return deps;
    }

    public static Set<String> getExternalDeps(final Scope main, final Scope provided) {
        Set<String> runtimeOnlyDeps = new HashSet<>(main.getExternalDeps());
        runtimeOnlyDeps.removeAll(provided.getExternalDeps());

        Set<String> deps = new HashSet<>(main.getExternalDeps());
        deps.removeAll(runtimeOnlyDeps);
        return deps;
    }

    public static Set<String> getExternalProvidedDeps(final Scope main, final Scope provided) {
        Set<String> deps = new HashSet<>(provided.getExternalDeps());
        deps.removeAll(main.getExternalDeps());
        return deps;
    }
}
