// IMyAidlInterface.aidl
package com.github.okbuilds.okbuck.example.common;

import com.github.okbuilds.okbuck.example.common.Person;

// Declare any non-default types here with import statements

interface IMyAidlInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);

    void sayHello(in Person person);
}
