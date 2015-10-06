/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.piasy.okbuck.helper

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

/**
 * helper class for android project.
 * */
class AndroidProjectHelper {
    /**
     * unknown: 0
     * */
    public static final int UNKNOWN = 0

    /**
     * Android application project: 1; com.android.build.gradle.AppPlugin was applied;
     * */
    public static final int ANDROID_APP_PROJECT = 1

    /**
     * Android library project: 2; com.android.build.gradle.LibraryPlugin was applied;
     * */
    public static final int ANDROID_LIB_PROJECT = 2

    /**
     * Java library project: 3; org.gradle.api.plugins.JavaPlugin was applied;
     * */
    public static final int JAVA_LIB_PROJECT = 3

    /**
     * get sub project type
     * */
    public static int getSubProjectType(Project project) {
        for (Plugin plugin : project.plugins) {
            if (plugin instanceof AppPlugin) {
                return ANDROID_APP_PROJECT
            } else if (plugin instanceof LibraryPlugin) {
                return ANDROID_LIB_PROJECT
            } else if (plugin instanceof JavaPlugin) {
                return JAVA_LIB_PROJECT
            }
        }

        return UNKNOWN
    }

}