package com.uber.okbuck.extension;

import java.util.Set;
import javax.annotation.Nullable;

public class TestExtension {

  /** Enable generation of robolectric test rules. */
  public boolean robolectric = false;

  /**
   * Choose only a specific subset of robolectric API levels to download. Default is all api levels
   */
  @Nullable public Set<String> robolectricApis = null;

  /** Enable generation of espresso test rules. */
  public boolean espresso = false;

  /** Enable generation of espresso test rules on android libraries. */
  public boolean espressoForLibraries = false;

  /** Enable creation of integrationTest rules for modules with `integrationTest` folder */
  public boolean enableIntegrationTests = false;
}
