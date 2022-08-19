package com.uber.depvalidator;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.util.io.FileSuffixes;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Objects;

/**
 * Represents a custom jar module containing uncompressed class data (derived from WALA's @see
 * JarStreamModule).
 */
public class JarDataModule implements Module {

  private final Iterable<JarClassReader.Entry> classIterable;

  /**
   * Constructs jar data module
   *
   * @param classIterable an interable containing class data (per-class jar entry name and bytes
   *     representing the class itself)
   */
  public JarDataModule(Iterable<JarClassReader.Entry> classIterable) {
    this.classIterable = classIterable;
  }

  @Override
  public Iterator<ModuleEntry> getEntries() {
    final Iterator<JarClassReader.Entry> it = classIterable.iterator();
    return new Iterator<ModuleEntry>() {

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public ModuleEntry next() {
        return new Entry(it.next());
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public String toString() {
    return "Jar data " + classIterable.toString();
  }

  @Override
  public int hashCode() {
    return classIterable.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    JarDataModule other = (JarDataModule) obj;
    return classIterable.equals(other.classIterable);
  }

  private class Entry implements ModuleEntry {

    private final JarClassReader.Entry readerEntry;

    Entry(JarClassReader.Entry readerEntry) {
      this.readerEntry = readerEntry;
    }

    @Override
    public String getName() {
      return readerEntry.name;
    }

    @Override
    public boolean isClassFile() {
      return true;
    }

    @Override
    public InputStream getInputStream() {
      return new ByteArrayInputStream(readerEntry.bytes);
    }

    @Override
    public boolean isModuleFile() {
      return false;
    }

    @Override
    public Module asModule() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "nested jar data entry: " + getName();
    }

    @Override
    public String getClassName() {
      return FileSuffixes.stripSuffix(getName());
    }

    @Override
    public boolean isSourceFile() {
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getContainer(), getName());
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Entry other = (Entry) obj;
      if (!getContainer().equals(other.getContainer())) {
        return false;
      }
      if (!getName().equals(other.getName())) {
        return false;
      }
      return true;
    }

    @Override
    public Module getContainer() {
      return JarDataModule.this;
    }
  }
}
