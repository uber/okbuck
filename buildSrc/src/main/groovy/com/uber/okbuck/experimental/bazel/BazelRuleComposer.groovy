package com.uber.okbuck.experimental.bazel

/**
 * This class contains Bazel-specific overrides of
 * {@link com.uber.okbuck.composer.base.BuckRuleComposer}. It is a mixin trait as opposed to a
 * subclass because there are existing subclasses of {@code BuckRuleComposer} that are extended by
 * their Bazel counterparts, e.g.
 * {@link com.uber.okbuck.composer.java.JavaBuckRuleComposer} and
 * {@link com.uber.okbuck.experimental.bazel.BazelJavaLibraryRuleComposer}.
 *
 * Any Bazel rule composer class that needs these overrides should implement this trait.
 */
trait BazelRuleComposer {
    // The okbuck cache contains a BUCK file at `cacheDirPath/BUCK`. We cannot put a BUILD file
    // there, because the JARs and AARs are also in that directory so we cannot create java_import
    // and aar_import rules with the same names. Instead we put the BUILD file in `cacheDirPath/..`.
    static Set<String> external(Set<String> deps) {
        return deps.collect { String dep -> "//okbazel:${dep.tokenize('/').last()}" }
    }
}
