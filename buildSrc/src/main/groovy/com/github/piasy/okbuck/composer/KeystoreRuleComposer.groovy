/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.piasy.okbuck.composer

import com.github.piasy.okbuck.model.AndroidAppTarget
import com.github.piasy.okbuck.util.FileUtil
import org.apache.commons.io.FileUtils

final class KeystoreRuleComposer {

    private KeystoreRuleComposer() {
        // no instance
    }

    static String compose(AndroidAppTarget target) {
        AndroidAppTarget.Keystore keystore = target.keystore
        if (keystore != null) {
            File storeDir = new File(".okbuck/keystore/${target.identifier.replaceAll(':', '_')}")
            storeDir.mkdirs()

            FileUtil.copyResourceToProject("keystore/BUCK_FILE", new File(storeDir, "BUCK"))

            String storeFileName = "${target.name}.keystore"
            String storeFilePropsName = "${target.name}.keystore.properties"

            File keyStoreCopy = new File(storeDir, storeFileName)
            FileUtils.copyFile(keystore.storeFile, keyStoreCopy)

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
