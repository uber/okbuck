package com.uber.transform.runner;

import com.android.annotations.NonNull;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformOutputProvider;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Constructor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class TransformRunnerTest {

    @NonNull @Mock private Transform transform;
    @NonNull @Mock private TransformOutputProvider outputProvider;
    @NonNull @Mock private Class<Transform> transformClass;
    @NonNull @Mock private Constructor transformConstructor;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(transformClass.newInstance()).thenReturn(transform);
        when(transformClass.getConstructor(any(Class.class))).thenReturn(transformConstructor);
        when(transformConstructor.newInstance(anyString())).thenReturn(transform);
    }
}
