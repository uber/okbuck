package com.uber.okbuck.core.model.android;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Keystore {

  static Keystore create(String storeFile, String alias, String storePassword, String keyPassword) {
    return new AutoValue_Keystore(storeFile, alias, storePassword, keyPassword);
  }

  public abstract String getStoreFile();

  public abstract String getAlias();

  public abstract String getStorePassword();

  public abstract String getKeyPassword();
}
