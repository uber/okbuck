package com.uber.okbuck.composer.base;

import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;

public abstract class BuckRuleComposer {

  public BuckRuleComposer() {}

  public static Set<String> external(Set<String> deps) {
    return deps.stream().map(BuckRuleComposer::fileRule).collect(Collectors.toSet());
  }

  public static Set<String> externalApt(Set<String> deps) {
    return external(deps);
  }

  @Nullable
  public static String fileRule(@Nullable String filePath) {
    if (filePath == null) {
      return null;
    }

    StringBuilder ext = new StringBuilder("//");
    ext.append(filePath);
    int ind = FilenameUtils.indexOfLastSeparator(filePath) + 2;
    if (ind >= 0) {
      return ext.replace(ind, ind + 1, ":").toString();
    }
    return ext.toString();
  }

  public static Set<String> targets(Set<Target> deps) {
    return deps.stream().map(BuckRuleComposer::targets).collect(Collectors.toSet());
  }

  private static String targets(Target dep) {
    return String.format("//%s:src_%s", dep.getPath(), dep.getName());
  }

  public static Set<String> targetsApt(Set<Target> deps) {
    return deps.stream()
        .filter(target -> target.getClass().equals(JvmTarget.class))
        .map(BuckRuleComposer::targets)
        .collect(Collectors.toSet());
  }

  public static String binTargets(Target dep) {
    return String.format("//%s:bin_%s", dep.getPath(), dep.getName());
  }
}
