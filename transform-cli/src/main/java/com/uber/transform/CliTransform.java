package com.uber.transform;

import com.android.annotations.NonNull;
import com.android.build.api.transform.Transform;
import com.uber.transform.loader.SystemClassLoader;
import com.uber.transform.runner.TransformRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Main entry point for the cli application to apply the transform.
 */
public class CliTransform {

    @NonNull private static final String PROPERTY_IN_JARS_DIR = "in_jars_dir";
    @NonNull private static final String PROPERTY_OUT_JARS_DIR = "out_jars_dir";
    @NonNull private static final String PROPERTY_CONFIG_FILE = "config_file";
    @NonNull private static final String PROPERTY_ANDROID_BOOTCLASSPATH = "android_bootclasspath";
    @NonNull private static final String PROPERTY_TRANSFORM_DEPENDENCIES_FILE = "transform_jars_file";
    @NonNull private static final String PROPERTY_TRANSFORM_CLASS = "transform_class";
    @NonNull private static final String DEPENDENCIES_SEPARATOR = ":";

    private CliTransform() { }

    /**
     * Main.
     *
     * @param args arguments.
     */
    public static void main(@NonNull String[] args) {
        if (args.length != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("No argument is expected. All parameters should be passed through java system properties.\n");
            sb.append(PROPERTY_IN_JARS_DIR + " : jars input directory\n");
            sb.append(PROPERTY_OUT_JARS_DIR + " : jars output directory\n");
            sb.append(PROPERTY_CONFIG_FILE + " : configuration file\n");
            sb.append(PROPERTY_ANDROID_BOOTCLASSPATH + " : android classpath\n");
            sb.append(PROPERTY_TRANSFORM_DEPENDENCIES_FILE + " : transform dependencies [optional]\n");
            sb.append(PROPERTY_TRANSFORM_CLASS + " : full qualified name for transform class\n");
            throw new IllegalArgumentException(sb.toString());
        }

        //Loading transform dependencies in this class loader to ensure this application to work properly
        final String[] postProcessDependencies =
                readDependenciesFileFromSystemPropertyVar(PROPERTY_TRANSFORM_DEPENDENCIES_FILE);
        if (postProcessDependencies.length > 0) {
            new SystemClassLoader().loadJarFiles(postProcessDependencies);
        }

        //Passing the dependencies to the transform runner.
        main(new TransformRunnerProvider() {
            @Override
            public TransformRunner provide() {

                //Reading config file.
                String configFilePath = System.getProperty(PROPERTY_CONFIG_FILE);

                //Reading input jar dir
                String inJarsDir = System.getProperty(PROPERTY_IN_JARS_DIR);

                //Reading output jar dir
                String outJarsDir = System.getProperty(PROPERTY_OUT_JARS_DIR);

                //Reading android classpaths
                String[] androidClasspaths = readDependenciesFromSystemPropertyVar(PROPERTY_ANDROID_BOOTCLASSPATH);

                //Creating TransformRunner class
                try {

                    Class<Transform> transformClass;
                    String transformClassName = System.getProperty(PROPERTY_TRANSFORM_CLASS);
                    if (transformClassName != null) {
                        transformClass = (Class<Transform>) Class.forName(transformClassName);
                    } else {
                        throw new IllegalArgumentException("No transform class defined.");
                    }

                    TransformRunner runner = new TransformRunner(
                            configFilePath, inJarsDir, outJarsDir, androidClasspaths, transformClass);

                    return runner;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Main method for testing.
     *
     * @param provider a provider for the transform runner.
     */
    static void main(@NonNull TransformRunnerProvider provider) {
        try {
            provider.provide().runTransform();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads a dependency file from the path contained in a java system property.
     * Dependency file contains jar file paths all colon separated.
     *
     * @param property the property with the file path to read.
     * @return an array with all the dependency jars.
     */
    @NonNull
    static String[] readDependenciesFileFromSystemPropertyVar(String property) {
        String envVarFilePath = System.getProperty(property);
        if (envVarFilePath != null && envVarFilePath.length() > 0) {
            try {
                //This part needs to be written in vanilla java, since no dependency is available here.
                BufferedReader reader = new BufferedReader(new FileReader(new File(envVarFilePath)));
                String line = reader.readLine();
                reader.close();
                String[] dependencies = line.split(DEPENDENCIES_SEPARATOR);
                return dependencies;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return new String[0];
        }
    }

    /**
     * Reads the dependencies from a java system property.
     * Dependency file paths should all be colon separated.
     *
     * @param property the property with the dependencies.
     * @return an array with all the dependency jars.
     */
    @NonNull
    static String[] readDependenciesFromSystemPropertyVar(@NonNull String property) {
        String depsEnvVar = System.getProperty(property);
        return depsEnvVar != null && depsEnvVar.length() > 0
                ? depsEnvVar.split(DEPENDENCIES_SEPARATOR)
                : new String[0];
    }

    /**
     * A provider for transforms, used for mainly for testing.
     */
    interface TransformRunnerProvider {

        /**
         * Returns the transform runner.
         *
         * @return the transform runner.
         */
        TransformRunner provide();
    }
}
