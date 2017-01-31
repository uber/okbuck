package com.uber.transform.utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Utility methods for testing.
 */
public class TestUtils {

    private TestUtils() { }

    /**
     * Returns a temporary file to be deleted at the end of the test.
     *
     * @param directory true whether the {@link File} is a directory, false otherwise.
     * @param exists true whether the {@link File} should exist, false otherwise. If true an empty file will be
     * created.
     * @return the newly created file.
     */
    public static File getTmpFile(boolean directory, boolean exists) {
        File file;
        try {
            file = new File(FileUtils.getTempDirectory(), UUID.randomUUID().toString());
            if (exists) {
                if (directory) {
                    file.mkdirs();
                } else {
                    file.createNewFile();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        file.deleteOnExit();
        return file;
    }
}
