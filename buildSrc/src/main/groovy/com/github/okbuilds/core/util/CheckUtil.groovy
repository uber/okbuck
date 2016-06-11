package com.github.okbuilds.core.util

import org.apache.commons.lang.StringUtils

final class CheckUtil {

    private CheckUtil() {
        // no instance
    }

    static void checkStringNotEmpty(String string, String message) throws RuntimeException {
        if (StringUtils.isEmpty(string)) {
            throw new IllegalArgumentException(message)
        }
    }

    static void checkNotNull(Object obj, String message) throws RuntimeException {
        if (obj == null) {
            throw new IllegalArgumentException(message)
        }
    }
}
