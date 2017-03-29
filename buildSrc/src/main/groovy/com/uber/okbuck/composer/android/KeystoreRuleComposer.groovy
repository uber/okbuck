package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.util.FileUtil

import java.nio.file.Files
import java.nio.file.StandardCopyOption

final class KeystoreRuleComposer {

    private KeystoreRuleComposer() {
        // no instance
    }

    static String compose(AndroidAppTarget target) {
        AndroidAppTarget.Keystore keystore = target.keystore
        if (keystore != null) {
            File storeDir = target.rootProject.file(".okbuck/keystore/${target.identifier.replaceAll(':', '_')}")
            storeDir.mkdirs()

            File tmp = File.createTempFile("okbuck", "keystore")
            FileUtil.copyResourceToProject("keystore/BUCK_FILE", tmp)
            new File(storeDir, "BUCK").text = tmp.text

            String storeFileName = "${target.name}.keystore"
            String storeFilePropsName = "${target.name}.keystore.properties"

            File keyStoreCopy = new File(storeDir, storeFileName)
            Files.copy(keystore.storeFile.toPath(), keyStoreCopy.toPath(), StandardCopyOption.REPLACE_EXISTING)

            PrintWriter writer = new PrintWriter(new FileOutputStream(new File(storeDir,
                    storeFilePropsName)))
            writer.println("key.alias=${keystore.alias}")
            writer.println("key.store.password=${keystore.storePassword}")
            writer.println("key.alias.password=${keystore.keyPassword}")
            writer.close()

            String store = "//.okbuck/keystore/${target.identifier.replaceAll(':', '_')}:key_store_${target.name}.keystore"
            return store
        } else {
            throw new IllegalStateException("${target.name} of ${target.path} has no signing config set!")
        }
    }
}
