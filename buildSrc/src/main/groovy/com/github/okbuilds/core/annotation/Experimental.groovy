package com.github.okbuilds.core.annotation

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.TYPE, ElementType.METHOD, ElementType.FIELD])
@interface Experimental {

    /**
     * Enables generation of {@code robolectric_test} rules
     */
    boolean robolectric = false
}
