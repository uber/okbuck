package com.uber.okbuck.transform;

import com.android.build.api.transform.Format;
import com.uber.okbuck.transform.JarsTransformOutputProvider;

import org.assertj.core.util.Files;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class JarsTransformOutputProviderTest {

    private static final String NAME =
            "buck-out/bin/app/java_classes_preprocess_in_bin_prodDebug/buck-out/gen/.okbuck/cache/"
                    + "__app.rxscreenshotdetector-release.aar#aar_prebuilt_jar__/classes.jar";

    private static final String OUTPUT =
            "buck-out/bin/app/java_classes_preprocess_out_bin_prodDebug/";

    private static final String EXPECTED_OUTPUT_JAR =
            "buck-out/bin/app/java_classes_preprocess_out_bin_prodDebug/buck-out/gen/.okbuck/cache/"
                    + "__app.rxscreenshotdetector-release.aar#aar_prebuilt_jar__/classes.jar";

    private File inputJarFile;
    private File outputJarFile;
    private File outputFolder;
    private JarsTransformOutputProvider provider;

    @Before
    public void setUp() throws Exception {
        File baseFolder = Files.newTemporaryFolder();
        this.inputJarFile = new File(baseFolder, NAME);
        this.outputJarFile = new File(baseFolder, EXPECTED_OUTPUT_JAR);
        this.outputFolder = new File(baseFolder, OUTPUT);
        this.provider = new JarsTransformOutputProvider(outputFolder);
    }

    @Test
    public void getContentLocation() throws Exception {
        File output = provider.getContentLocation(inputJarFile.getAbsolutePath(), null, null, Format.JAR);
        assertThat(output.getAbsolutePath()).isEqualTo(outputJarFile.getAbsolutePath());
    }
}
