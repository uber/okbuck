package com.uber.okbuck.template.core;

import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.RockerTemplate;
import com.fizzed.rocker.runtime.DefaultRockerTemplate;
import java.util.Collection;

public abstract class RuleTemplate extends DefaultRockerTemplate {

  protected String ruleType;
  protected String name;
  protected boolean fileConfiguredVisibility;
  protected Collection visibility;
  protected Collection deps;
  protected Collection labels;
  protected Collection extraBuckOpts;

  public RuleTemplate(RockerModel model) {
    super(model);
    if (model instanceof Rule) {
      Rule rule = (Rule) model;
      this.ruleType = rule.ruleType;
      this.name = rule.name;
      this.fileConfiguredVisibility = rule.fileConfiguredVisibility;
      this.visibility = rule.visibility;
      this.deps = rule.deps;
      this.labels = rule.labels;
      this.extraBuckOpts = rule.extraBuckOpts;
    } else {
      throw new IllegalArgumentException(
          "Unable to create template (model was not an instance of " + Rule.class.getName() + ")");
    }
  }

  @Override
  protected void __associate(RockerTemplate context) {
    super.__associate(context);

    if (context instanceof RuleTemplate) {
      RuleTemplate ninjaContext = (RuleTemplate) context;
      this.ruleType = ninjaContext.ruleType;
      this.name = ninjaContext.name;
      this.visibility = ninjaContext.visibility;
      this.fileConfiguredVisibility = ninjaContext.fileConfiguredVisibility;
      this.deps = ninjaContext.deps;
      this.labels = ninjaContext.labels;
      this.extraBuckOpts = ninjaContext.extraBuckOpts;
    } else {
      throw new IllegalArgumentException(
          "Unable to associate (context was not an instance of "
              + RuleTemplate.class.getCanonicalName()
              + ")");
    }
  }
}
