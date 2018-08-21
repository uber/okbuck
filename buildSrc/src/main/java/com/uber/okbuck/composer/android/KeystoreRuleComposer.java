package com.uber.okbuck.composer.android;

import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.template.android.KeystoreRule;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class KeystoreRuleComposer extends BuckRuleComposer {

  private static final String STORE_FILE = "app.keystore";
  private static final String STORE_FILE_PROPS = "app.keystore.properties";

  private KeystoreRuleComposer() {
    // no instance
  }

  @Nullable
  public static String compose(final AndroidAppTarget target) {
    AndroidAppTarget.Keystore keystore = target.getKeystore();
    if (keystore != null) {
      Path keyStoreGen = keystore.getPath().toPath().resolve(STORE_FILE);
      try {
        Files.copy(
            keystore.getStoreFile().toPath(), keyStoreGen, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException ignored) {
      }

      new KeystoreRule()
          .alias(keystore.getAlias())
          .storePassword(keystore.getStorePassword())
          .keyPassword(keystore.getKeyPassword())
          .render(keystore.getPath().toPath().resolve(STORE_FILE_PROPS));

      return fileRule(
          FileUtil.getRelativePath(target.getRootProject().getProjectDir(), keyStoreGen.toFile()));
    } else {
      throw new IllegalStateException(
          target.getName() + " of " + target.getPath() + " has no signing config set!");
    }
  }
}
