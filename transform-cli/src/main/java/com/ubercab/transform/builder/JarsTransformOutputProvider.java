package com.ubercab.transform.builder;

import com.android.annotations.NonNull;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.TransformOutputProvider;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;

/**
 * A {@link TransformOutputProvider} providing jar outputs based on a given output folder.
 */
public class JarsTransformOutputProvider implements TransformOutputProvider {

    @NonNull private final File outputFolder;
    @NonNull private final String[] outputFolderParts;

    /**
     * Constructor.
     */
    public JarsTransformOutputProvider(@NonNull File outputFolder) {
        this.outputFolder = outputFolder;
        this.outputFolderParts = outputFolder.getAbsolutePath().split(File.separator);
    }

    @Override
    public void deleteAll() throws IOException {
        FileUtils.deleteDirectory(outputFolder);
        outputFolder.mkdirs();
    }

    @Override
    @NonNull
    public File getContentLocation(
            @NonNull String name,
            @NonNull Set<QualifiedContent.ContentType> types,
            @NonNull Set<? super QualifiedContent.Scope> scopes,
            @NonNull Format format) {

        //Just a temp directory not to be used, to make the transform happy.
        if (format == Format.DIRECTORY) {
            return FileUtils.getTempDirectory();
        }

        /**
         * For jars, the name is actually the absolute path.
         * The goal here is to calculate the absolute output path starting from the input one. Since The output path
         * will be different only in the last folder (replaced from the base output folder), it's possible to exclude
         * from the input path the parts in common plus the last folder (i.e. all the parts of the output folder).
         *
         * Example of input path:
         * ...mobile/okbuck/buck-out/bin/app
         * /java_classes_preprocess_in_bin_prodDebug/buck-out/gen/.okbuck/cache/__app.rxscreenshotdetector-release
         * .aar#aar_prebuilt_jar__/classes.jar
         *
         * Example of output base folder:
         * ...mobile/okbuck/buck-out/bin/app/
         * java_classes_preprocess_out_bin_prodDebug/
         *
         * Example of output path:
         * ...mobile/okbuck/buck-out/bin/app
         * /java_classes_preprocess_out_bin_prodDebug/buck-out/gen/.okbuck/cache/__app.rxscreenshotdetector-release
         * .aar#aar_prebuilt_jar__/classes.jar
         */

        String[] nameParts = name.split(File.separator);
        LinkedList<String> baseFolderParts = new LinkedList(Arrays.asList(nameParts));
        for (int i = 0; i < outputFolderParts.length; i++) {
            baseFolderParts.removeFirst();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < baseFolderParts.size(); i++) {
            sb.append(baseFolderParts.get(i));
            if (i != baseFolderParts.size() - 1) {
                sb.append(File.separator);
            }
        }
        return new File(outputFolder, sb.toString());
    }
}
