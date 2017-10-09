package com.uber.okbuck.composer.android

import com.uber.okbuck.composer.base.BuckRuleComposer
import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.util.FileUtil
import com.uber.okbuck.template.android.KeystoreRule

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

final class KeystoreRuleComposer extends BuckRuleComposer {

    private static final String STORE_FILE = "app.keystore"
    private static final String STORE_FILE_PROPS = "app.keystore.properties"

    private KeystoreRuleComposer() {
        // no instance
    }

    static String compose(AndroidAppTarget target) {
        AndroidAppTarget.Keystore keystore = target.keystore
        if (keystore != null) {
            Path keyStoreGen = keystore.path.toPath().resolve(STORE_FILE)
            try {
                Files.copy(keystore.storeFile.toPath(), keyStoreGen, StandardCopyOption.REPLACE_EXISTING)
            } catch (IOException ignored) { }

            new KeystoreRule()
                    .alias(keystore.alias)
                    .storePassword(keystore.storePassword)
                    .keyPassword(keystore.keyPassword)
                    .render(keystore.path.toPath().resolve(STORE_FILE_PROPS))

            return fileRule(FileUtil.getRelativePath(target.rootProject.projectDir, keyStoreGen.toFile()))
        } else {
            throw new IllegalStateException("${target.name} of ${target.path} has no signing config set!")
        }
    }
}
