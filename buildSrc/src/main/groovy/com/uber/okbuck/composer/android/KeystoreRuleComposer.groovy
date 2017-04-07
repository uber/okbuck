package com.uber.okbuck.composer.android

import com.uber.okbuck.composer.base.BuckRuleComposer
import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.util.FileUtil

import java.nio.file.Files
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
            File keyStoreGen = new File(keystore.path, STORE_FILE)
            Files.copy(keystore.storeFile.toPath(), keyStoreGen.toPath(), StandardCopyOption.REPLACE_EXISTING)

            PrintWriter writer = new PrintWriter(new FileOutputStream(new File(keystore.path, STORE_FILE_PROPS)))
            writer.println("key.alias=${keystore.alias}")
            writer.println("key.store.password=${keystore.storePassword}")
            writer.println("key.alias.password=${keystore.keyPassword}")
            writer.close()

            return fileRule(FileUtil.getRelativePath(target.rootProject.projectDir, keyStoreGen))
        } else {
            throw new IllegalStateException("${target.name} of ${target.path} has no signing config set!")
        }
    }
}
