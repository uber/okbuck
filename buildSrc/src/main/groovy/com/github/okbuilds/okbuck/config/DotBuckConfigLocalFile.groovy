package com.github.okbuilds.okbuck.config

final class DotBuckConfigLocalFile extends BuckConfigFile {

    private final Map<String, String> mAliases
    private final String mBuildToolVersion
    private final String mTarget
    private final List<String> mIgnore

    DotBuckConfigLocalFile(Map<String, String> aliases, String buildToolVersion, String target, List<String> ignore) {
        mAliases = aliases
        mBuildToolVersion = buildToolVersion
        mTarget = target
        mIgnore = ignore
    }

    @Override
    final void print(PrintStream printer) {
        printer.println("[alias]")
        mAliases.each { alias, target ->
            printer.println("\t${alias} = ${target}")
        }
        printer.println()

        printer.println("[android]")
        printer.println("\tbuild_tools_version = ${mBuildToolVersion}")
        printer.println("\ttarget = ${mTarget}")
        printer.println()

        printer.println("[project]")
        printer.print("\tignore = ${mIgnore.join(', ')}")
        printer.println()
    }
}
