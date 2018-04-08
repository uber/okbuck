package com.uber.okbuck.generator;

import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.template.config.BuckConfig;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class BuckConfigLocalGenerator {

    private BuckConfigLocalGenerator() {}

    /**
     * generate {@link BuckConfig}
     */
    public static BuckConfig generate(
            OkBuckExtension okbuck,
            @Nullable String groovyHome,
            @Nullable String kotlinHome,
            @Nullable String scalaHome,
            @Nullable String proguardJar,
            Set<String> defs) {

        return new BuckConfig()
                .buildToolsVersion(okbuck.buildToolVersion)
                .target(okbuck.target)
                .groovyHome(groovyHome)
                .kotlinHome(kotlinHome)
                .scalaHome(scalaHome)
                .proguardJar(proguardJar)
                .defs(defs);
    }
}
