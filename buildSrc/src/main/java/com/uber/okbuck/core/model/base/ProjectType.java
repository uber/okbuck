package com.uber.okbuck.core.model.base;

public enum ProjectType {
  ANDROID_APP,
  ANDROID_LIB,
  JAVA_LIB,
  GROOVY_LIB(RuleType.GROOVY_LIBRARY, RuleType.GROOVY_TEST),
  KOTLIN_LIB(RuleType.KOTLIN_LIBRARY, RuleType.KOTLIN_TEST),
  SCALA_LIB(RuleType.SCALA_LIBRARY, RuleType.SCALA_TEST),
  UNKNOWN;

  private final RuleType mainRuleType;
  private final RuleType testRuleType;

  ProjectType() {
    this(RuleType.JAVA_LIBRARY, RuleType.JAVA_TEST);
  }

  ProjectType(RuleType mainRuleType, RuleType testRuleType) {
    this.mainRuleType = mainRuleType;
    this.testRuleType = testRuleType;
  }

  public RuleType getMainRuleType() {
    return mainRuleType;
  }

  public RuleType getTestRuleType() {
    return testRuleType;
  }
}
