package com.uber.okbuck.composer.android;

import com.google.common.base.Preconditions;
import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.android.Keystore;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.android.KeystoreRule;
import com.uber.okbuck.template.core.Rule;
import javax.annotation.Nullable;

public final class KeystoreRuleComposer extends AndroidBuckRuleComposer {

  private KeystoreRuleComposer() {
    // no instance
  }

  @Nullable
  public static Rule compose(AndroidAppTarget target) {
    Keystore keystore = target.getKeystore();
    if (keystore != null) {
      return new KeystoreRule()
          .storeFile(Preconditions.checkNotNull(fileRule(keystore.getStoreFile())))
          .storePassword(keystore.getStorePassword())
          .keyAlias(keystore.getKeyAlias())
          .keyPassword(keystore.getKeyPassword())
          .ruleType(RuleType.KEYSTORE.getBuckName())
          .name(keystore(target));
    } else {
      throw new IllegalStateException(
          target.getName() + " of " + target.getPath() + " has no signing config set!");
    }
  }
}
