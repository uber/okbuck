package com.uber.okbuck.rule.base;

import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.RockerTemplate;
import com.fizzed.rocker.runtime.DefaultRockerTemplate;

import java.util.Set;

public abstract class RuleTemplate extends DefaultRockerTemplate {

    protected String ruleType;
    protected String name;
    protected Set<String> visibility;
    protected Set<String> deps;
    protected Set<String> extraBuckOpts;

    public RuleTemplate(RockerModel model) {
        super(model);
        if (model instanceof Rule) {
            Rule rule = (Rule) model;
            this.ruleType = rule.ruleType;
            this.name = rule.name;
            this.visibility = rule.visibility;
            this.deps = rule.deps;
            this.extraBuckOpts = rule.extraBuckOpts;
        } else {
            throw new IllegalArgumentException("Unable to create template (model was not an instance of " +
                    Rule.class.getName() + ")");
        }
    }

    @Override
    protected void __associate(RockerTemplate context) {
        super.__associate(context);

        if (context instanceof RuleTemplate) {
            RuleTemplate ninjaContext = (RuleTemplate)context;
            this.ruleType = ninjaContext.ruleType;
            this.name = ninjaContext.name;
            this.visibility = ninjaContext.visibility;
            this.deps = ninjaContext.deps;
            this.extraBuckOpts = ninjaContext.extraBuckOpts;
        } else {
            throw new IllegalArgumentException("Unable to associate (context was not an instance of " +
                    RuleTemplate.class.getCanonicalName() + ")");
        }
    }
}
