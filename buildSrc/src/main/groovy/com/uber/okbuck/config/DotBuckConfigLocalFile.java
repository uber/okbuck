package com.uber.okbuck.config;

import com.uber.okbuck.core.io.Printer;

import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Set;

public final class DotBuckConfigLocalFile extends BuckConfigFile {

    private final Map<String, String> aliases;
    private final String buildToolVersion;
    private final String target;
    private final String groovyHome;
    private final String kotlinHome;
    private final String scalaHome;
    private final String proguardJar;
    private final Set<String> defs;

    public DotBuckConfigLocalFile(
            Map<String, String> aliases,
            String buildToolVersion,
            String target,
            String groovyHome,
            String kotlinHome,
            String scalaHome,
            String proguardJar,
            Set<String> defs) {
        this.aliases = aliases;
        this.buildToolVersion = buildToolVersion;
        this.target = target;
        this.groovyHome = groovyHome;
        this.kotlinHome = kotlinHome;
        this.scalaHome = scalaHome;
        this.proguardJar = proguardJar;
        this.defs = defs;
    }

    @Override
    public final void print(Printer printer) {
        printer.println("[alias]");
        aliases.forEach((alias, target) -> printer.println("\t" + alias + " = " + target));
        printer.println();

        printer.println("[android]");
        printer.println("\tbuild_tools_version = " + buildToolVersion);
        printer.println("\ttarget = " + target);
        printer.println();

        if (!defs.isEmpty()) {
            printer.println("[buildfile]");
            printer.println("\tincludes = " + String.join(" ", defs));
            printer.println();
        }

        if (!StringUtils.isEmpty(groovyHome)) {
            printer.println("[groovy]");
            printer.println("\tgroovy_home = " + groovyHome);
            printer.println();
        }

        if (!StringUtils.isEmpty(kotlinHome)) {
            printer.println("[kotlin]");
            printer.println("\tkotlin_home = " + kotlinHome);
            printer.println();
        }

        if (!StringUtils.isEmpty(scalaHome)) {
            printer.println("[scala]");
            printer.println("\tcompiler = //" + scalaHome + ":scala-compiler");
            printer.println("\tlibrary = //" + scalaHome + ":scala-library");
            printer.println();
        }

        if (!StringUtils.isEmpty(proguardJar)) {
            printer.println("[tools]");
            printer.println("\tproguard = " + proguardJar);
            printer.println();
        }
    }
}
