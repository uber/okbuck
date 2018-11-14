package com.uber.okbuck.core.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.extension.RuleOverridesExtension;
import com.uber.okbuck.template.core.Rule;
import java.util.List;
import java.util.Map;

public final class LoadStatementsUtil {

  private static final String RES_GLOB = "res_glob";
  private static final String SUBDIR_GLOB = "subdir_glob";

  private LoadStatementsUtil() {}

  public static Multimap<String, String> getLoadStatements(
      List<Rule> rules, OkBuckExtension okBuckExtension) {
    return getLoadStatements(rules, okBuckExtension.getRuleOverridesExtension());
  }

  public static Multimap<String, String> getLoadStatements(
      List<Rule> rules, RuleOverridesExtension ruleOverridesExtension) {
    Multimap<String, String> loadStatements = TreeMultimap.create();
    Map<String, RuleOverridesExtension.OverrideSetting> overrides =
        ruleOverridesExtension.getOverrides();
    for (Rule rule : rules) {
      // Android resource template requires res_glob function from buck defs
      if (RuleType.ANDROID_RESOURCE.getBuckName().equals(rule.ruleType())) {
        loadStatements.put(OkBuckGradlePlugin.OKBUCK_DEFS_TARGET, RES_GLOB);
        loadStatements.put(OkBuckGradlePlugin.OKBUCK_DEFS_TARGET, SUBDIR_GLOB);
      }
      if (overrides.containsKey(rule.ruleType())) {
        RuleOverridesExtension.OverrideSetting setting = overrides.get(rule.ruleType());
        loadStatements.put(setting.getImportLocation(), setting.getNewRuleName());
        rule.ruleType(setting.getNewRuleName());
      }
    }
    return loadStatements;
  }
}
