package com.uber.okbuck.composer.java;

import com.google.common.collect.ImmutableList;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer;
import com.uber.okbuck.template.common.ExportFile;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.LintBinary;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LintBinaryComposer extends JvmBuckRuleComposer {

  /**
   * @param dependencies External Dependencies whose rule needs to be created
   * @return List of rules
   */
  public static List<Rule> compose(Set<String> dependencies, Collection<String> files) {
    ImmutableList.Builder<Rule> rulesBuilder = new ImmutableList.Builder<>();

    rulesBuilder.add(new LintBinary().deps(BuckRuleComposer.external(dependencies)));

    rulesBuilder.addAll(
        files.stream().sorted().map(f -> new ExportFile().name(f)).collect(Collectors.toList()));

    return rulesBuilder.build();
  }
}
