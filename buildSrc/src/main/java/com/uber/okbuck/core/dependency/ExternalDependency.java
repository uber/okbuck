package com.uber.okbuck.core.dependency;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * Represents a pre packaged dependency from an external
 * source like gradle/maven cache or the filesystem
 */
public final class ExternalDependency {

    private static final String LOCAL_DEP_VERSION = "1.0.0-LOCAL";
    private static final String SOURCES_JAR = "-sources.jar";
    /**
     * this is used to output artifacts as %GROUP%--%NAME%--%VERSION%
     * Thus making it possible to re-construct maven coordinates.
     */
    private static final String CACHE_DELIMITER = "--";

    public final boolean isLocal;
    public final String version;
    public final File depFile;
    public final String group;
    public final String name;
    public final VersionlessDependency versionless;
    public final String packaging;

    public ExternalDependency(String group, String name, String version, File depFile) {
        this(group, name, version, depFile, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        ExternalDependency that = (ExternalDependency) o;

        return version.equals(that.version) &&
                group.equals(that.group) &&
                name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = version.hashCode();
        result = 31 * result + group.hashCode();
        result = 31 * result + name.hashCode();
        return result;
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
                return group + CACHE_DELIMITER + name + CACHE_DELIMITER + String.valueOf(version) + "." + packaging;
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
            this.version = version;
        } else {
            this.version = LOCAL_DEP_VERSION;
        }

        this.depFile = depFile;
        this.versionless = new VersionlessDependency(group, name);
        this.packaging = FilenameUtils.getExtension(depFile.getName());
    }

    public static ExternalDependency fromLocal(File localDep) {
        String baseName = FilenameUtils.getBaseName(localDep.getName());
        return new ExternalDependency(baseName, baseName, LOCAL_DEP_VERSION, localDep, true);
    }

    public static final class VersionlessDependency {

        private final String group;
        private final String name;

        public VersionlessDependency(String group, String name) {
            this.group = group;
            this.name = name;
        }

        public String getGroup() {
            return group;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }

            VersionlessDependency that = (VersionlessDependency) o;

            return group.equals(that.group) && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            int result = group.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }
    }
}
