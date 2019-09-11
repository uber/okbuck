package com.uber.okbuck.core.model.base;

public enum ProjectType {
  ANDROID_APP,
  ANDROID_LIB,
  JAVA_LIB,
  GROOVY_LIB(RuleType.GROOVY_LIBRARY, RuleType.GROOVY_TEST, RuleType.GROOVY_INTEGRATION_TEST),
  KOTLIN_LIB(RuleType.KOTLIN_LIBRARY, RuleType.KOTLIN_TEST, RuleType.KOTLIN_INTEGRATION_TEST),
  SCALA_LIB(RuleType.SCALA_LIBRARY, RuleType.SCALA_TEST, RuleType.SCALA_INTEGRATION_TEST),
  UNKNOWN;

  private final RuleType mainRuleType;
  private final RuleType testRuleType;
  private final RuleType integrationTestRuleType;

  ProjectType() {
    this(RuleType.JAVA_LIBRARY, RuleType.JAVA_TEST, RuleType.JAVA_INTEGRATION_TEST);
  }

  ProjectType(RuleType mainRuleType, RuleType testRuleType, RuleType integrationTestRuleType) {
    this.mainRuleType = mainRuleType;
    this.testRuleType = testRuleType;
    this.integrationTestRuleType = integrationTestRuleType;
  }

  public RuleType getMainRuleType() {
    return mainRuleType;
  }

  public RuleType getTestRuleType() {
    return testRuleType;
  }

  public RuleType getIntegrationTestRuleType() {
    return integrationTestRuleType;
  }
}
