package com.uber.okbuck.core.dependency;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Represents a pre packaged dependency from an external
 * source like gradle/maven cache or the filesystem
 */
public final class ExternalDependency implements Comparable<ExternalDependency> {

    private static final String LOCAL_DEP_VERSION = "1.0.0-LOCAL";
    private static final String SOURCES_JAR = "-sources.jar";

    public final boolean isLocal;
    public final DefaultArtifactVersion version;
    public final File depFile;
    public final String group;
    public final String name;
    public final VersionlessDependency versionless;

    public ExternalDependency(String group, String name, String version, File depFile) {
        this(group, name, version, depFile, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        ExternalDependency that = (ExternalDependency) o;

        if (!version.equals(that.version)) { return false; }
        if (!group.equals(that.group)) { return false; }
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = version.hashCode();
        result = 31 * result + group.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public int compareTo(@NotNull ExternalDependency o) {
        return version.compareTo(o.version);
    }

    @Override
    public String toString() {
        return this.group + ":" + this.name + ":" + String.valueOf(this.version) + " -> " + this.depFile.toString();
    }

    public String getCacheName() {
        return getCacheName(true);
    }

    public String getCacheName(boolean useFullDepName) {
        if (useFullDepName) {
            if (!StringUtils.isEmpty(group)) {
                return group + "." + depFile.getName();
            } else {
                return name + "." + depFile.getName();
            }

        } else {
            return depFile.getName();
        }
    }

    public String getSourceCacheName(boolean useFullDepName) {
        return getCacheName(useFullDepName).replaceFirst("\\.(jar|aar)$", SOURCES_JAR);
    }

    private ExternalDependency(String group, String name, String version, File depFile, boolean isLocal) {
        this.group = group;
        this.name = name;
        this.isLocal = isLocal;
        if (!StringUtils.isEmpty(version)) {
            this.version = new DefaultArtifactVersion(version);
        } else {
            this.version = new DefaultArtifactVersion(LOCAL_DEP_VERSION);
        }

        this.depFile = depFile;
        this.versionless = new VersionlessDependency(group, name);
    }

    public static ExternalDependency fromLocal(File localDep) {
        String baseName = FilenameUtils.getBaseName(localDep.getName());
        return new ExternalDependency(baseName, baseName, LOCAL_DEP_VERSION, localDep, true);
    }

    public static final class VersionlessDependency {

        private final String group;
        private final String name;

        VersionlessDependency(String group, String name) {
            this.group = group;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }

            VersionlessDependency that = (VersionlessDependency) o;

            if (!group.equals(that.group)) { return false; }
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            int result = group.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }
    }
}
