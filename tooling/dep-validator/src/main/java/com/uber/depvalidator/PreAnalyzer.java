package com.uber.depvalidator;

import com.google.common.collect.ImmutableSet;
import com.ibm.wala.types.TypeName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;

/**
 * Implements pre-analysis to discover certain types of information from a straightforward bytecode
 * analysis.
 */
public class PreAnalyzer {

  protected static final String APP_PKG_PREFIX = "com/uber";

  private static final int CP_CLASS = 7;

  private static final boolean BY_PKG_NAME = true;

  private static final int CLASS_EXT_LEN = new String(".class").length();

  private final List<String> rootJarsInfo;
  private final Set<TypeName> rootClasses;

  private final List<String> extJarsInfo;
  private final Map<TypeName, String> extClassJarInfos;

  private final List<String> allJarsInfo;
  private final Map<TypeName, List<TypeName>> classRefs;

  /**
   * Maps each class to a set of classes it references but multiple classes with the same name can
   * be present so this needs to be qualifed with a string representing a jar file. In other words
   * it is a jarinfo -> class_name -> referenced_classes map.
   */
  private final Map<String, Map<TypeName, List<TypeName>>> classRefsCache;

  /**
   * Constructs the pre-analyzer.
   *
   * @param rootJarsInfo whitespace-separated tuples describing post-commit jars implementing logic
   *     of a given service: content hash and path
   * @param extJarsInfo whitespace-separated tuples describing post-commit third-party library jars:
   *     content hash and path
   * @param allJarsInfo whitespace-separated tuples describing all post-commit jars: content hash
   *     and path
   */
  public PreAnalyzer(List<String> rootJarsInfo, List<String> extJarsInfo, List<String> allJarsInfo)
      throws Exception {
    this.rootJarsInfo = rootJarsInfo;
    this.extJarsInfo = extJarsInfo;
    this.allJarsInfo = allJarsInfo;
    this.classRefsCache = new HashMap<>();
    this.rootClasses = computeRootClasses();
    this.extClassJarInfos = computeExtClassJarInfos();
    this.classRefs = computeClassRefs();
  }

  /**
   * Computes a set post-commit classes implementing logic of a given service.
   *
   * @return a set of root classes
   */
  public Set<TypeName> getRootClasses() throws Exception {
    return rootClasses;
  }

  private Set<TypeName> computeRootClasses() throws Exception {
    ImmutableSet.Builder<TypeName> rootClassesBuilder = new ImmutableSet.Builder<>();
    for (String jarInfo : rootJarsInfo) {
      JarClassReader jarReader = JarClassReader.getReader(jarInfo);
      for (JarClassReader.Entry classData : jarReader) {
        ClassReader classReader = new ClassReader(classData.bytes);
        String className = classReader.getClassName();
        String pkgName = getPkgNameFromClassName(className);
        if (!BY_PKG_NAME || pkgName.startsWith(APP_PKG_PREFIX)) {
          rootClassesBuilder.add(TypeName.findOrCreate("L" + className));
        }
      }
    }
    return rootClassesBuilder.build();
  }

  /**
   * Computes a map from post-commit third-party library classes to jar file info string (hash and
   * path) defining these methods.
   *
   * @return third-party classes to jar file info map
   */
  public Map<TypeName, String> getExtClassJarInfos() throws Exception {
    return extClassJarInfos;
  }

  private Map<TypeName, String> computeExtClassJarInfos() throws Exception {
    Map<TypeName, String> extClassJarInfosMap = new HashMap<>();
    for (String jarInfo : extJarsInfo) {
      JarClassReader jarReader = JarClassReader.getReader(jarInfo);
      for (JarClassReader.Entry classData : jarReader) {
        ClassReader classReader = new ClassReader(classData.bytes);
        String className = "L" + classReader.getClassName();
        // there may be many classes in the external jar files that
        // have the same name (e.g. because there are multiple
        // versions of the same library), but only the last one ends
        // up in the fat jar, so we keep replacing jar info in the map
        // until we are done with all the jar files
        extClassJarInfosMap.put(TypeName.findOrCreate(className), jarInfo);
      }
    }
    return extClassJarInfosMap;
  }

  /**
   * Returns a mapping from classes to the classes references.
   *
   * @return a referent to referee set mapping
   */
  public Map<TypeName, List<TypeName>> getClassRefs() {
    return classRefs;
  }

  private void addClassAndRefs(
      byte[] classBytes, String name, String jarInfo, Map<TypeName, List<TypeName>> classRefsMap) {
    // name always ends with ".class" is it's obtained from jar file
    // entry with this very prefix (see JarClassReader.hasNext method)
    String className = "L" + name.substring(0, name.length() - CLASS_EXT_LEN);
    TypeName classTypeName = TypeName.findOrCreate(className);
    if (!classRefsMap.containsKey(classTypeName)) {
      // classes are processed in order they are encountered on the
      // ordered jar file list and only the first class encountered
      // should be recorded along with its references (the others will
      // not make it to the fat jar)
      List<TypeName> refs;
      // references of a given class might have been computed before -
      // consult the cache
      Map<TypeName, List<TypeName>> cachedClassesMap = classRefsCache.get(jarInfo);
      if (cachedClassesMap == null) {
        cachedClassesMap = new HashMap<>();
        classRefsCache.put(jarInfo, cachedClassesMap);
        refs = null;
      } else {
        refs = cachedClassesMap.get(classTypeName);
      }
      if (refs == null) {
        ClassReader classReader = new ClassReader(classBytes);
        char[] buf = new char[classReader.getMaxStringLength()];
        refs = new ArrayList<>();
        for (int i = 1; i < classReader.getItemCount(); i++) {
          int itemOffset = classReader.getItem(i);
          if (itemOffset > 0 && classReader.readByte(itemOffset - 1) == CP_CLASS) {
            String depName = "L" + classReader.readUTF8(itemOffset, buf);
            refs.add(TypeName.findOrCreate(depName));
          }
        }
        cachedClassesMap.put(classTypeName, refs);
      }
      classRefsMap.put(classTypeName, refs);
    }
  }

  private Map<TypeName, List<TypeName>> computeClassRefs() throws Exception {
    Set<TypeName> liveClassesSet = new HashSet<>();
    Map<TypeName, List<TypeName>> classRefsMap = new HashMap<>();

    for (String jarInfo : allJarsInfo) {
      JarClassReader jarReader = JarClassReader.getReader(jarInfo);
      for (JarClassReader.Entry classData : jarReader) {
        addClassAndRefs(classData.bytes, classData.name, jarInfo, classRefsMap);
      }
    }
    return classRefsMap;
  }

  private String getPkgNameFromClassName(String pkgName) {
    if (pkgName.indexOf("/") != -1) {
      return pkgName.substring(0, pkgName.lastIndexOf("/"));
    } else {
      return "";
    }
  }
}
