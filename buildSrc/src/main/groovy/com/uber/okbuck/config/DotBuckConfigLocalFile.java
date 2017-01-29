package com.uber.okbuck.config;

import com.uber.okbuck.OkBuckGradlePlugin;

import org.apache.commons.lang.StringUtils;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

public final class DotBuckConfigLocalFile extends BuckConfigFile {

    private final Map<String, String> aliases;
    private final String buildToolVersion;
    private final String target;
    private final List<String> ignore;
    private final String groovyHome;
    private final String proguardJar;

    public DotBuckConfigLocalFile(
            Map<String, String> aliases,
            String buildToolVersion,
            String target,
            List<String> ignore,
            String groovyHome,
            String proguardJar) {
        this.aliases = aliases;
        this.buildToolVersion = buildToolVersion;
        this.target = target;
        this.ignore = ignore;
        this.groovyHome = groovyHome;
        this.proguardJar = proguardJar;
    }

    @Override
    public final void print(PrintStream printer) {
        printer.println("[alias]");
        aliases.forEach((alias, target) -> printer.println("\t" + alias + " = " + target));
        printer.println();

        printer.println("[android]");
        printer.println("\tbuild_tools_version = " + buildToolVersion);
        printer.println("\ttarget = " + target);
        printer.println();

        printer.println("[project]");
        printer.print("\tignore = " + String.join(",", ignore));
        printer.println();

        printer.println("[buildfile]");
        printer.print("\tincludes = //" + OkBuckGradlePlugin.OKBUCK_DEFS);
        printer.println();

        if (!StringUtils.isEmpty(groovyHome)) {
            printer.println("[groovy]");
            printer.print("\tgroovy_home = " + groovyHome);
            printer.println();
        }

        if (!StringUtils.isEmpty(proguardJar)) {
            printer.println("[tools]");
            printer.print("\tproguard = " + proguardJar);
            printer.println();
        }
    }
}
