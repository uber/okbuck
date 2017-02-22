package com.uber.okbuck.config;

import org.apache.commons.lang.StringUtils;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DotBuckConfigLocalFile extends BuckConfigFile {

    private final Map<String, String> aliases;
    private final String buildToolVersion;
    private final String target;
    private final List<String> ignore;
    private final String groovyHome;
    private final String kotlinCompiler;
    private final String kotlinRuntime;
    private final String proguardJar;
    private final Set<String> defs;

    public DotBuckConfigLocalFile(
            Map<String, String> aliases,
            String buildToolVersion,
            String target,
            List<String> ignore,
            String groovyHome,
            String kotlinCompiler,
            String kotlinRuntime,
            String proguardJar,
            Set<String> defs) {
        this.aliases = aliases;
        this.buildToolVersion = buildToolVersion;
        this.target = target;
        this.ignore = ignore;
        this.groovyHome = groovyHome;
        this.kotlinCompiler = kotlinCompiler;
        this.kotlinRuntime = kotlinRuntime;
        this.proguardJar = proguardJar;
        this.defs = defs;
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
        printer.println("\tignore = " + String.join(", ", ignore));
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

        if (!StringUtils.isEmpty(kotlinCompiler) && !StringUtils.isEmpty(kotlinRuntime)) {
            printer.println("[kotlin]");
            printer.println("\tcompiler = " + kotlinCompiler);
            printer.println("\truntime_jar = " + kotlinRuntime);
            printer.println();
        }

        if (!StringUtils.isEmpty(proguardJar)) {
            printer.println("[tools]");
            printer.println("\tproguard = " + proguardJar);
            printer.println();
        }
    }
}
