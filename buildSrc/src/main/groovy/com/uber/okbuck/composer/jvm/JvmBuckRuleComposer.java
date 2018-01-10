package com.uber.okbuck.composer.jvm;

import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.Target;
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

    /**
     * Get api & implementation target deps.
     * runtimeOnlyDeps = runtimeClasspath(api + implementation + runtimeOnly) -
     *                   compileClasspath(api + implementation + compileOnly)
     * deps = runtimeClasspath(api + implementation + runtimeOnly) - runtimeOnlyDeps
     * @param runtime RuntimeClasspath scope
     * @param compile CompileClasspath scope
     * @return Target deps
     */
    public static Set<Target> getTargetDeps(final Scope runtime, final Scope compile) {
        Set<Target> runtimeOnlyDeps = new HashSet<>(runtime.getTargetDeps());
        runtimeOnlyDeps.removeAll(compile.getTargetDeps());

        Set<Target> deps = new HashSet<>(runtime.getTargetDeps());
        deps.removeAll(runtimeOnlyDeps);
        return deps;
    }

    /**
     * Get compileOnly target deps.
     * compileOnlyDeps = compileClasspath(api + implementation + compileOnly) -
     *                   runtimeClasspath(api + implementation + runtimeOnly)
     * @param runtime RuntimeClasspath scope
     * @param compile CompileClasspath scope
     * @return CompileOnly Target deps
     */
    public static Set<Target> getTargetProvidedDeps(final Scope runtime, final Scope compile) {
        Set<Target> deps = new HashSet<>(compile.getTargetDeps());
        deps.removeAll(runtime.getTargetDeps());
        return deps;
    }

    /**
     * Get api & implementation external deps.
     * runtimeOnlyDeps = runtimeClasspath(api + implementation + runtimeOnly) -
     *                   compileClasspath(api + implementation + compileOnly)
     * deps = runtimeClasspath(api + implementation + runtimeOnly) - runtimeOnlyDeps
     * @param runtime RuntimeClasspath scope
     * @param compile CompileClasspath scope
     * @return External deps
     */
    public static Set<String> getExternalDeps(final Scope runtime, final Scope compile) {
        Set<String> runtimeOnlyDeps = new HashSet<>(runtime.getExternalDeps());
        runtimeOnlyDeps.removeAll(compile.getExternalDeps());

        Set<String> deps = new HashSet<>(runtime.getExternalDeps());
        deps.removeAll(runtimeOnlyDeps);
        return deps;
    }

    /**
     * Get compileOnly external deps.
     * compileOnlyDeps = compileClasspath(api + implementation + compileOnly) -
     *                   runtimeClasspath(api + implementation + runtimeOnly)
     * @param runtime RuntimeClasspath scope
     * @param compile CompileClasspath scope
     * @return CompileOnly Target deps
     */
    public static Set<String> getExternalProvidedDeps(final Scope runtime, final Scope compile) {
        Set<String> deps = new HashSet<>(compile.getExternalDeps());
        deps.removeAll(runtime.getExternalDeps());
        return deps;
    }
}
