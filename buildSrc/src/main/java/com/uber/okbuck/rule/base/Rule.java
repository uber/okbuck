package com.uber.okbuck.rule.base;

import com.fizzed.rocker.runtime.DefaultRockerModel;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

public abstract class Rule<T extends Rule> extends DefaultRockerModel {

    protected String ruleType;
    protected String name;
    protected Set<String> visibility = ImmutableSet.of();
    protected Set<String> deps = ImmutableSet.of();
    protected Set<String> extraBuckOpts = ImmutableSet.of();

    public T ruleType(String ruleType) {
        this.ruleType = ruleType;
        return (T) this;
    }

    public T name(String name) {
        this.name = name;
        return (T) this;
    }

    public T deps(Set<String> deps) {
        this.deps = deps;
        return (T) this;
    }

    public T visibility(Set<String> visibility) {
        this.visibility = visibility;
        return (T) this;
    }

    public T extraBuckOpts(Set<String> extraBuckOpts) {
        this.extraBuckOpts = extraBuckOpts;
        return (T) this;
    }
}
