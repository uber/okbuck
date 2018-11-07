package com.uber.okbuck.template.core;

import com.fizzed.rocker.runtime.DefaultRockerModel;
import com.fizzed.rocker.runtime.OutputStreamOutput;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

@SuppressWarnings("unchecked")
public abstract class Rule<T extends Rule> extends DefaultRockerModel {

  private static final ImmutableSet<String> DEFAULT_VISIBILITY = ImmutableSet.of("PUBLIC");

  protected String ruleType = "";
  protected String name = "";
  protected boolean fileConfiguredVisibility = false;
  protected Collection visibility = ImmutableSet.of();
  protected Collection deps = ImmutableSet.of();
  protected Collection labels = ImmutableSet.of();
  protected Collection extraBuckOpts = ImmutableSet.of();

  public String name() {
    return name;
  }

  public T name(String name) {
    this.name = name;
    return (T) this;
  }

  public String buckName() {
    return ":" + name;
  }

  public T ruleType(String ruleType) {
    this.ruleType = ruleType;
    return (T) this;
  }

  public String ruleType() {
    return ruleType;
  }

  public T deps(Collection deps) {
    this.deps = deps;
    return (T) this;
  }

  public T labels(Collection labels) {
    this.labels = labels;
    return (T) this;
  }

  public T fileConfiguredVisibility(boolean enable) {
    this.fileConfiguredVisibility = enable;
    return (T) this;
  }

  public T visibility(Collection visibility) {
    this.visibility = visibility;
    return (T) this;
  }

  public T defaultVisibility() {
    this.visibility = DEFAULT_VISIBILITY;
    return (T) this;
  }

  public T extraBuckOpts(Collection extraBuckOpts) {
    this.extraBuckOpts = extraBuckOpts;
    return (T) this;
  }

  protected static boolean valid(Map m) {
    return m != null && !m.isEmpty();
  }

  protected static boolean valid(Collection c) {
    return c != null && !c.isEmpty();
  }

  protected static boolean valid(String s) {
    return s != null && !s.isEmpty();
  }

  protected static boolean valid(Integer i) {
    return i != null;
  }

  public void render(OutputStream os) {
    render((contentType, charsetName) -> new OutputStreamOutput(contentType, os, charsetName));
  }

  public void render(Path path) {
    render(path.toFile());
  }

  public void render(File file) {
    try {
      file.getParentFile().mkdirs();
      render(new FileOutputStream(file));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  protected static ImmutableSortedSet<String> sorted(Collection c) {
    ImmutableSortedSet.Builder<String> builder =
        new ImmutableSortedSet.Builder<>(String::compareTo);
    for (Object o : c) {
      builder.add(o.toString());
    }
    return builder.build();
  }
}
