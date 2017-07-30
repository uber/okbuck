package com.uber.okbuck.core.dependency;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.File;

/**
 * Represents a pre packaged dependency from an external
 * source like gradle/maven cache or the filesystem
 */
public final class ExternalDependency {

    private static final String LOCAL_DEP_VERSION = "1.0.0";
    private static final String SOURCES_JAR = "-sources.jar";

    public final boolean isLocal;
    public final DefaultArtifactVersion version;
    public final File depFile;
    public final String group;
    public final String name;

    public ExternalDependency(String group, String name, String version, File depFile) {
        this(group, name, version, depFile, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        ExternalDependency that = (ExternalDependency) o;

        if (isLocal != that.isLocal) { return false; }
        if (version != null ? !version.equals(that.version) : that.version != null) { return false; }
        if (depFile != null ? !depFile.equals(that.depFile) : that.depFile != null) { return false; }
        if (group != null ? !group.equals(that.group) : that.group != null) { return false; }
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result = version != null ? version.hashCode() : 0;
        result = 31 * result + (depFile != null ? depFile.hashCode() : 0);
        result = 31 * result + (group != null ? group.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (isLocal ? 1 : 0);
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
    }

    public static ExternalDependency fromLocal(File localDep) {
        String baseName = FilenameUtils.getBaseName(localDep.getName());
        return new ExternalDependency(baseName, baseName, LOCAL_DEP_VERSION, localDep, true);
    }
}
