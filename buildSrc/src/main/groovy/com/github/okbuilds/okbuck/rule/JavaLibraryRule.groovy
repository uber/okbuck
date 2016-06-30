package com.github.okbuilds.okbuck.rule

final class JavaLibraryRule extends JavaRule {

    JavaLibraryRule(String name, List<String> visibility, List<String> deps,
                    Set<String> srcSet, Set<String> annotationProcessors,
                    Set<String> aptDeps, Set<String> providedDeps, String sourceCompatibility,
                    String targetCompatibility, List<String> postprocessClassesCommands,
                    List<String> options) {
        super("java_library", name, visibility, deps, srcSet, annotationProcessors,
                aptDeps, providedDeps, sourceCompatibility, targetCompatibility,
                postprocessClassesCommands, options)
    }
}
