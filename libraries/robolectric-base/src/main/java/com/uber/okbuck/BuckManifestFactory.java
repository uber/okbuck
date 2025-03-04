package com.uber.okbuck;

import org.robolectric.annotation.Config;
import org.robolectric.internal.ManifestFactory;
import org.robolectric.internal.ManifestIdentifier;
import org.robolectric.res.Fs;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class BuckManifestFactory implements ManifestFactory {

  private final String buckManifest;
  private final String buckResourcesProperty;

  public BuckManifestFactory(String buckManifest, String buckResourcesProperty) {
    this.buckManifest = buckManifest;
    this.buckResourcesProperty = buckResourcesProperty;
  }

  @Override
  public ManifestIdentifier identify(Config config) {
    final List<String> buckResources =
        Arrays.asList(buckResourcesProperty.split(File.pathSeparator));

    final String packageName = config.packageName();
    final Path res = Fs.fromUrl(buckResources.get(buckResources.size() - 1));
    final Path assets = Fs.fromUrl(buckResources.get(buckResources.size() - 1));
    final Path manifest = Fs.fromUrl(buckManifest);

    final Set<ManifestIdentifier> libraries = new LinkedHashSet<>();
    ListIterator<String> it = buckResources.listIterator(buckResources.size());
    while (it.hasPrevious()) {
      libraries.add(new ManifestIdentifier(packageName, manifest, Fs.fromUrl(it.previous()), assets,
          Collections.emptyList()));
    }

    return new ManifestIdentifier(packageName, manifest, res, assets, new ArrayList<>(libraries));
  }
}
