package com.github.okbuilds.okbuck.rule

final class JavaTestRule extends JavaRule {

    JavaTestRule(String name, List<String> visibility, List<String> deps,
                 Set<String> srcSet, Set<String> annotationProcessors,
                 Set<String> annotationProcessorDeps, String sourceCompatibility,
                 String targetCompatibility, List<String> postprocessClassesCommands,
                 List<String> options) {
        super("java_test", name, visibility, deps, srcSet, annotationProcessors,
                annotationProcessorDeps, sourceCompatibility, targetCompatibility,
                postprocessClassesCommands, options)
    }
}
