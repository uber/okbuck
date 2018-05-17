package com.uber.okbuck;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.Fs;
import org.robolectric.res.FsFile;
import org.robolectric.res.ResourcePath;

/**
 * The example test runner only runs robolectric tests with buck. If you want to run unit tests with
 * both gradle and buck, you can use the example test runner and create a hybrid with robolectric's
 * {@link org.robolectric.RobolectricGradleTestRunner}.
 */
public class BuckRobolectricTestRunner extends RobolectricTestRunner {

  private static final String ROBOLECTRIC_RESOURCE_DIRECTORIES = "buck.robolectric_res_directories";
  private static final String ROBOLECTRIC_MANIFEST = "buck.robolectric_manifest";

  public BuckRobolectricTestRunner(Class<?> testClass) throws InitializationError {
    super(testClass);
  }

  @Override
  protected AndroidManifest getAppManifest(Config config) {
    String buckManifest = System.getProperty(ROBOLECTRIC_MANIFEST);
    String buckResourcesProperty = System.getProperty(ROBOLECTRIC_RESOURCE_DIRECTORIES);

    if (buckManifest != null && buckResourcesProperty != null) {
      final List<String> buckResources =
          Arrays.asList(buckResourcesProperty.split(File.pathSeparator));

      final FsFile res = Fs.fileFromPath(buckResources.get(buckResources.size() - 1));
      final FsFile assets = Fs.fileFromPath(buckResources.get(buckResources.size() - 1));
      final FsFile manifest = Fs.fileFromPath(buckManifest);

      return new AndroidManifest(manifest, res, assets, config.packageName()) {

        @Override
        public List<ResourcePath> getIncludedResourcePaths() {
          Collection<ResourcePath> resourcePaths = new LinkedHashSet<>();
          resourcePaths.add(super.getResourcePath());

          ListIterator<String> it = buckResources.listIterator(buckResources.size());
          while (it.hasPrevious()) {
            resourcePaths.add(
                new ResourcePath(
                    getRClass(),
                    getPackageName(),
                    Fs.fileFromPath(it.previous()),
                    getAssetsDirectory()));
          }
          return new ArrayList<>(resourcePaths);
        }
      };
    } else {
      return null;
    }
  }
}
