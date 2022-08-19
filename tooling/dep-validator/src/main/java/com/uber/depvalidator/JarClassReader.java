package com.uber.depvalidator;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import javax.annotation.Nullable;

/**
 * Implements a reader supporting iteration over all class file data in a given jar file. Assumes
 * single-threaded execution.
 */
public class JarClassReader implements Iterable<JarClassReader.Entry> {

  // size of a buffer used to incrementally read class data from a jar file entry
  private static final int CLASS_DATA_BUF = 1024;

  // maps jar file hashes to jar file readers
  private static final Map<String, JarClassReader> readerCache = new HashMap<>();

  private final JarInputStream jarInputStream;

  private final boolean cachingEnabled;

  // caches (for re-use) class data retrieved by the jar class reader iterator
  private final List<Entry> jarDataCache;

  /**
   * Constructs the jar file reader.
   *
   * @param filePath jar file path
   * @param cachingEnabled should jar data be cached or not
   */
  private JarClassReader(String filePath, boolean cachingEnabled) throws IOException {
    this.jarInputStream = new JarInputStream(new FileInputStream(filePath));
    this.cachingEnabled = cachingEnabled;
    this.jarDataCache = new ArrayList<>();
  }

  /*
   * Jar class reader factory method.
   *
   * @param jarInfo whitespace-separated string containing jar file
   * content hash and path
   * @return jar class reader
   */
  public static JarClassReader getReader(String jarInfo) throws IOException {
    String[] jarInfoArray = Analyzer.getJarInfoArray(jarInfo);
    String fileHash = jarInfoArray[0];
    String filePath = jarInfoArray[1];

    if (filePath.contains(Analyzer.EXT_DIR)) {
      // cache only third-party jars
      JarClassReader reader = readerCache.get(fileHash);
      if (reader == null) {
        reader = new JarClassReader(filePath, true);
        readerCache.put(fileHash, reader);
      }
      return reader;
    } else {
      return new JarClassReader(filePath, false);
    }
  }

  @Override
  public Iterator<Entry> iterator() {
    if (cachingEnabled && jarDataCache.size() > 0) {
      // return values collected during initial (and only) iteration
      return jarDataCache.iterator();
    }
    return new JarClassIterator();
  }

  static class Entry {
    final String name;
    final byte[] bytes;

    Entry(String name, byte[] bytes) {
      this.name = name;
      this.bytes = bytes;
    }
  }

  private class JarClassIterator implements Iterator<Entry> {

    @Nullable private JarEntry currentEntry;

    JarClassIterator() {
      this.currentEntry = null;
    }

    @Override
    public boolean hasNext() {
      if (currentEntry != null) {
        return true;
      } else {
        try {
          JarEntry entry = jarInputStream.getNextJarEntry();
          while (entry != null) {
            if (entry.getName().endsWith(".class")) {
              currentEntry = entry;
              return true;
            }
            entry = jarInputStream.getNextJarEntry();
          }
        } catch (IOException x) {
          // fall into returning false
        }
        return false;
      }
    }

    @Override
    public Entry next() throws NoSuchElementException {
      if (currentEntry == null) {
        throw new NoSuchElementException("no more jar file entries");
      } else {
        try {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          byte[] buf = new byte[CLASS_DATA_BUF];
          int bytes = jarInputStream.read(buf);
          while (bytes != -1) {
            out.write(buf, 0, bytes);
            bytes = jarInputStream.read(buf);
          }
          Entry entry = new Entry(currentEntry.getName(), out.toByteArray());
          jarDataCache.add(entry);
          return entry;
        } catch (IOException ex) {
          throw new NoSuchElementException("error getting jar file entry");
        } finally {
          currentEntry = null;
        }
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
