/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Piasy
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

package com.github.okbuilds.okbuck.example;

import android.support.multidex.MultiDex;
import android.widget.Toast;
import com.facebook.buck.android.support.exopackage.ExopackageApplication;
import com.github.okbuilds.okbuck.example.common.Calc;
import com.github.okbuilds.okbuck.example.common.CalcMonitor;

public class AppShell extends ExopackageApplication {

    private final boolean mIsExopackageMode;

    private static final String APP_NAME = "com.github.okbuilds.okbuck.example.MyApp";

    public AppShell() {
        super(APP_NAME, BuildConfig.EXOPACKAGE_FLAGS != 0);
        mIsExopackageMode = BuildConfig.EXOPACKAGE_FLAGS != 0;
    }

    @Override
    protected void onBaseContextAttached() {
        if (!mIsExopackageMode) {
            MultiDex.install(this);
        }
        Calc calc = new Calc(new CalcMonitor(this));
        Toast.makeText(this, "calc: " + calc.add(1, 2), Toast.LENGTH_SHORT).show();
    }
}
