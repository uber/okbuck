package com.fizzed.rocker.gradle;

import java.io.File;

import org.gradle.api.PathValidation;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

/**
 * Copy of https://github.com/fizzed/rocker/blob/master/rocker-gradle-plugin/src/main/java/com/fizzed/rocker/gradle/RockerConfiguration.java from 1.3.0 with fixed annotations
 * Can remove file once https://github.com/fizzed/rocker/pull/157 is landed and upgraded in dependencies.
 *
 * Bean for the configuration options of Rocker Compiler
 */
public class RockerConfiguration {
  private Project project;
  private boolean skip = false;
  private boolean failOnError = true;
  private boolean skipTouch = true;
  private String touchFile;
  private String javaVersion;
  private String extendsClass;
  private String extendsModelClass;
  private Boolean optimize;
  private Boolean discardLogicWhitespace;
  private String targetCharset;
  private String suffixRegex;
  private File outputBaseDirectory;
  private File classBaseDirectory;
  private String[] postProcessing;
  private Boolean markAsGenerated;

  public RockerConfiguration(Project project) {
    super();
    this.project = project;
  }

  @Internal
  public boolean isSkip() {
    return skip;
  }

  public void setSkip(boolean skip) {
    this.skip = skip;
  }

  @Internal
  public boolean isFailOnError() {
    return failOnError;
  }

  public void setFailOnError(boolean failOnError) {
    this.failOnError = failOnError;
  }

  @Internal
  public boolean isSkipTouch() {
    return skipTouch;
  }

  public void setSkipTouch(boolean skipTouch) {
    this.skipTouch = skipTouch;
  }

  @Optional
  @Input
  public String getTouchFile() {
    return touchFile;
  }

  public void setTouchFile(String touchFile) {
    this.touchFile = touchFile;
  }

  @Optional
  @Input
  public String getJavaVersion() {
    return javaVersion;
  }

  public void setJavaVersion(String javaVersion) {
    this.javaVersion = javaVersion;
  }

  @Optional
  @Input
  public String getExtendsClass() {
    return extendsClass;
  }

  public void setExtendsClass(String extendsClass) {
    this.extendsClass = extendsClass;
  }

  @Optional
  @Input
  public String getExtendsModelClass() {
    return extendsModelClass;
  }

  public void setExtendsModelClass(String extendsModelClass) {
    this.extendsModelClass = extendsModelClass;
  }

  @Optional
  @Input
  public Boolean getOptimize() {
    return optimize;
  }

  public void setOptimize(Boolean optimize) {
    this.optimize = optimize;
  }

  @Optional
  @Input
  public Boolean getDiscardLogicWhitespace() {
    return discardLogicWhitespace;
  }

  public void setDiscardLogicWhitespace(Boolean discardLogicWhitespace) {
    this.discardLogicWhitespace = discardLogicWhitespace;
  }

  @Optional
  @Input
  public String getTargetCharset() {
    return targetCharset;
  }

  public void setTargetCharset(String targetCharset) {
    this.targetCharset = targetCharset;
  }

  @Optional
  @Input
  public String getSuffixRegex() {
    return suffixRegex;
  }

  public void setSuffixRegex(String suffixRegex) {
    this.suffixRegex = suffixRegex;
  }

  @Internal
  public File getOutputBaseDirectory() {
    return outputBaseDirectory;
  }

  public void setOutputBaseDirectory(Object outputBaseDirectory) {
    this.outputBaseDirectory = project.file(outputBaseDirectory);
  }

  public void setOutputBaseDirectory(Object outputBaseDirectory, PathValidation pv) {
    this.outputBaseDirectory = project.file(outputBaseDirectory, pv);
  }

  @Input // Neither input nor output directory, but generated rocker.conf depends on it
  public String getClassBaseDirectoryPath() {
    return classBaseDirectory.getAbsolutePath();
  }

  @Internal
  public File getClassBaseDirectory() {
    return classBaseDirectory;
  }

  public void setClassBaseDirectory(Object classBaseDirectory) {
    this.classBaseDirectory = project.file(classBaseDirectory);
  }

  public void setClassBaseDirectory(Object classBaseDirectory, PathValidation pv) {
    this.classBaseDirectory = project.file(classBaseDirectory, pv);
  }

  @Optional
  @Input
  public String[] getPostProcessing() {
    return postProcessing;
  }

  public void setPostProcessing(String[] postProcessing) {
    this.postProcessing = postProcessing;
  }

  @Optional
  @Input
  public Boolean getMarkAsGenerated() {
    return markAsGenerated;
  }

  public void setMarkAsGenerated(Boolean markAsGenerated) {
    this.markAsGenerated = markAsGenerated;
  }
}
