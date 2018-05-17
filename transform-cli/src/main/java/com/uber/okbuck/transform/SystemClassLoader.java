package com.uber.okbuck.transform;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Extend the support for the system class loader to load additional dependencies at runtime. Note
 * that internally it works using {@link ClassLoader#getSystemClassLoader()}, making accessible the
 * `addURL` method through reflection.
 */
class SystemClassLoader {

  private final URLClassLoader systemClassLoader;
  private final Method addUrlMethod;

  /** Constructor. */
  SystemClassLoader() {
    try {
      this.systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
      this.addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
      this.addUrlMethod.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Loads a new jar in the system class loader using the given path.
   *
   * @param jarFilePath the path of a jar file.
   */
  private void loadJarFile(String jarFilePath) {
    try {
      File file = new File(jarFilePath);
      if (!file.exists()) {
        throw new FileNotFoundException(file.getAbsolutePath());
      }
      URL jarFileUrl = file.toURI().toURL();
      this.addUrlMethod.invoke(systemClassLoader, jarFileUrl);
      System.out.println("Added dependency: " + jarFileUrl.toString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Loads new jars in the system class loader using the given paths.
   *
   * @param jarFilePaths the paths of a jar file.
   */
  void loadJarFiles(String[] jarFilePaths) {
    for (String jarFilePath : jarFilePaths) {
      this.loadJarFile(jarFilePath);
    }
  }
}
