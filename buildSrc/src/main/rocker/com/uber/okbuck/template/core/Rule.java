package com.uber.okbuck.template.core;

import com.fizzed.rocker.runtime.DefaultRockerModel;
import com.fizzed.rocker.runtime.OutputStreamOutput;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public abstract class Rule<T extends Rule> extends DefaultRockerModel {

    private static Set<String> DEFAULT_VISIBILITY = ImmutableSet.of("PUBLIC");

    protected String ruleType;
    protected String name;
    protected Collection visibility = ImmutableSet.of();
    protected Collection deps = ImmutableSet.of();
    protected Collection labels = ImmutableSet.of();
    protected Collection extraBuckOpts = ImmutableSet.of();

    public String name() {
        return name;
    }

    public T ruleType(String ruleType) {
        this.ruleType = ruleType;
        return (T) this;
    }

    public T name(String name) {
        this.name = name;
        return (T) this;
    }

    public T deps(Collection deps) {
        this.deps = deps;
        return (T) this;
    }

    public T labels(Collection labels) {
        this.labels = labels;
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

    public void render(OutputStream os) {
        render((contentType, charsetName) -> new OutputStreamOutput(contentType, os, charsetName));
    }

    public void render(File file) {
        try {
            file.getParentFile().mkdirs();
            render(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected static Collection<String> sorted(Collection c) {
        ImmutableSortedSet.Builder<String> builder = new ImmutableSortedSet.Builder<>(String::compareTo);
        for (Object o : c) {
            builder.add(o.toString());
        }
        return builder.build();
    }
}
