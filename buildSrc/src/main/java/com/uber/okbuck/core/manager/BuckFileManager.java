package com.uber.okbuck.core.manager;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.extension.RuleOverridesExtension;
import com.uber.okbuck.template.common.LoadStatements;
import com.uber.okbuck.template.core.Rule;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BuckFileManager {

  private static final byte[] NEWLINE = System.lineSeparator().getBytes(StandardCharsets.UTF_8);
  private static final String RES_GLOB = "res_glob";
  private static final String SUBDIR_GLOB = "subdir_glob";

  private final RuleOverridesExtension ruleOverridesExtension;

  public BuckFileManager(RuleOverridesExtension ruleOverridesExtension) {
    this.ruleOverridesExtension = ruleOverridesExtension;
  }

  public void writeToBuckFile(List<Rule> rules, File buckFile) {
    this.writeToBuckFile(rules, buckFile, TreeMultimap.create());
  }

  public void writeToBuckFile(
      List<Rule> rules, File buckFile, Multimap<String, String> extraLoadStatements) {
    if (!rules.isEmpty()) {
      Multimap<String, String> loadStatements = getLoadStatements(rules);
      loadStatements.putAll(extraLoadStatements);
      File parent = buckFile.getParentFile();
      if (!parent.exists() && !parent.mkdirs()) {
        throw new IllegalStateException("Couldn't create dir: " + parent);
      }

      try (OutputStream fos = new FileOutputStream(buckFile);
          BufferedOutputStream os = new BufferedOutputStream(fos)) {

        if (!loadStatements.isEmpty()) {
          LoadStatements.template(writableLoadStatements(loadStatements)).render(os);
        }

        for (int index = 0; index < rules.size(); index++) {
          // Don't add a new line before the first rule
          if (index != 0) {
            os.write(NEWLINE);
          }
          rules.get(index).render(os);
        }
      } catch (IOException e) {
        throw new IllegalStateException("Couldn't create the buck file", e);
      }
    }
  }

  private Multimap<String, String> getLoadStatements(List<Rule> rules) {
    Multimap<String, String> loadStatements = TreeMultimap.create();
    Map<String, RuleOverridesExtension.OverrideSetting> overrides =
        ruleOverridesExtension.getOverrides();
    for (Rule rule : rules) {
      // Android resource template requires res_glob function from buck defs
      if (RuleType.ANDROID_RESOURCE.getBuckName().equals(rule.ruleType())) {
        loadStatements.put(OkBuckGradlePlugin.OKBUCK_TARGETS_TARGET, RES_GLOB);
        loadStatements.put(OkBuckGradlePlugin.OKBUCK_TARGETS_TARGET, SUBDIR_GLOB);
      }
      if (overrides.containsKey(rule.ruleType())) {
        RuleOverridesExtension.OverrideSetting setting = overrides.get(rule.ruleType());
        loadStatements.put(setting.getImportLocation(), setting.getNewRuleName());
        rule.ruleType(setting.getNewRuleName());
      }
    }
    return loadStatements;
  }

  private static List<String> writableLoadStatements(Multimap<String, String> loadStatements) {
    return loadStatements
        .asMap()
        .entrySet()
        .stream()
        .map(
            loadStatement ->
                Stream.concat(Stream.of(loadStatement.getKey()), loadStatement.getValue().stream())
                    .map(statement -> "'" + statement + "'")
                    .collect(Collectors.joining(", ", "load(", ")")))
        .collect(Collectors.toList());
  }
}
