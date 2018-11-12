package com.uber.okbuck.core.model.android;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Keystore {

  static Keystore create(
      String storeFile, String storePassword, String keyAlias, String keyPassword) {
    return new AutoValue_Keystore(storeFile, storePassword, keyAlias, keyPassword);
  }

  public abstract String getStoreFile();

  public abstract String getStorePassword();

  public abstract String getKeyAlias();

  public abstract String getKeyPassword();
}
