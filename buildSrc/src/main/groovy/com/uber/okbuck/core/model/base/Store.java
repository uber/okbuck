package com.uber.okbuck.core.model.base;

import com.uber.okbuck.core.util.FileUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A persistent data store for simple string key/value pairs
 * that auto cleans up entries that are no longer accessed.
 */
public final class Store {

    private final Set<String> accessed;
    private final File storeFile;
    private final Properties props;

    public Store(File storeFile) {
        this.storeFile = storeFile;
        props = new Properties();
        accessed = ConcurrentHashMap.newKeySet();

        if (storeFile.exists()) {
            try {
                props.load(new BufferedInputStream(new FileInputStream(storeFile)));
            } catch (IOException ignored) { }
        }
    }

    @Nullable
    public String get(@NotNull String key) {
        accessed.add(key);
        return props.getProperty(key);
    }

    public void set(@NotNull String key, @NotNull String val) {
        accessed.add(key);
        props.setProperty(key, val);
    }

    public void persist() {
        if (props.isEmpty()) {
            FileUtil.deleteQuietly(storeFile.toPath());
            return;
        }
        try {
            if (!storeFile.exists()) {
                storeFile.getParentFile().mkdirs();
                storeFile.createNewFile();
            }
            props.keySet().retainAll(accessed);
            FileOutputStream out = new FileOutputStream(storeFile);
            ByteArrayOutputStream arrayOut = new ByteArrayOutputStream();
            props.store(arrayOut, null);
            String string = new String(arrayOut.toByteArray(), "8859_1");
            String sep = System.getProperty("line.separator");
            String content = string.substring(string.indexOf(sep) + sep.length());
            out.write(content.getBytes("8859_1"));
        } catch (IOException ignored) {}
    }
}
