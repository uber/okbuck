package com.uber.depvalidator;

import com.google.common.collect.ImmutableSet;
import com.ibm.wala.types.TypeName;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Collects classes specified as part of the jar file's metadata. */
public class AdditionalClassDependencyReader {

  private static final String META_SERVICES_DIR = "META-INF/services/";

  // maps jar file hashes to sets of classes defined in the jar file's metadata
  private final Map<String, Set<TypeName>> depCache;

  /** Constructs reader of additional classes that the service depends on. */
  public AdditionalClassDependencyReader() throws IOException {
    this.depCache = new HashMap<>();
  }

  private Set<TypeName> computeJarDependencies(String filePath) throws Exception {
    ImmutableSet.Builder<TypeName> deps = new ImmutableSet.Builder<>();
    JarFile jarFile = new JarFile(filePath);
    Enumeration<JarEntry> entries = jarFile.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      deps.addAll(getServiceLoaderDependencies(entry, jarFile));
    }
    return deps.build();
  }

  /**
   * Computes the set of class dependencies in the jar file's metadata.
   *
   * @param jarsInfo whitespace-separated tuples describing a set of jars: content hash and path
   * @return metadata class dependencies
   */
  public Set<TypeName> getDependencies(List<String> jarsInfo) throws Exception {
    ImmutableSet.Builder<TypeName> res = new ImmutableSet.Builder<>();
    for (String jarInfo : jarsInfo) {
      String[] jarInfoArray = Analyzer.getJarInfoArray(jarInfo);
      String fileHash = jarInfoArray[0];
      String filePath = jarInfoArray[1];
      Set<TypeName> deps;
      if (filePath.contains(Analyzer.EXT_DIR)) {
        // cache only third-party jars as they are likely shared the most
        deps = depCache.get(fileHash);
        if (deps == null) {
          deps = computeJarDependencies(filePath);
          depCache.put(fileHash, deps);
        }
      } else {
        deps = computeJarDependencies(filePath);
      }
      res.addAll(deps);
    }
    return res.build();
  }

  // TODO: refactor into separate dependency readers once we have more than one

  private TypeName getTypeRefFromDotted(String name) {
    return TypeName.findOrCreate("L" + name.replaceAll("\\.", "/"));
  }

  private Set<TypeName> getServiceLoaderDependencies(JarEntry entry, JarFile jarFile)
      throws Exception {
    // As per https://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html
    // we may have a file in the services directory whose name represents an interface
    // and whose content represents implementations of this interface
    // We need to add these to the class dependencies that can be violated
    ImmutableSet.Builder<TypeName> deps = new ImmutableSet.Builder<>();
    String name = entry.getName();
    if (name.contains(META_SERVICES_DIR)) {
      String[] tmp = name.split(META_SERVICES_DIR);
      // consider only files that are directly under META_SERVICES_DIR dir
      if (tmp.length == 2 && tmp[1].indexOf("/") == -1) {
        // consider only the case when the directory is non-empty
        // (the file name representing the interface contains "."-s instead of "/"-s)
        deps.add(getTypeRefFromDotted(tmp[1]));
        return (Set)
            Analyzer.getLinesFromFile(
                new InputStreamReader(jarFile.getInputStream(entry), Charset.forName("UTF-8")),
                deps,
                (s) -> {
                  return getTypeRefFromDotted(s);
                });
      }
    }
    return Collections.emptySet();
  }
}
