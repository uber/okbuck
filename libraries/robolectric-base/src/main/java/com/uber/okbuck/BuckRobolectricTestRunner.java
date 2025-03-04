package com.uber.okbuck;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ManifestFactory;

/**
 * The example test runner only runs robolectric tests with buck. If you want to run unit tests with
 * both gradle and buck, you can use the example test runner and create a hybrid with robolectric's
 * {@link org.robolectric.RobolectricTestRunner}.
 */
public class BuckRobolectricTestRunner extends RobolectricTestRunner {

  private static final String ROBOLECTRIC_RESOURCE_DIRECTORIES = "buck.robolectric_res_directories";
  private static final String ROBOLECTRIC_MANIFEST = "buck.robolectric_manifest";

  public BuckRobolectricTestRunner(Class<?> testClass) throws InitializationError {
    super(testClass);
  }

  @Override
  protected ManifestFactory getManifestFactory(Config config) {
    String buckManifest = System.getProperty(ROBOLECTRIC_MANIFEST);
    String buckResourcesProperty = System.getProperty(ROBOLECTRIC_RESOURCE_DIRECTORIES);

    if (buckManifest != null && buckResourcesProperty != null) {
      return new BuckManifestFactory(buckManifest, buckResourcesProperty);
    }

    return super.getManifestFactory(config);
  }
}
