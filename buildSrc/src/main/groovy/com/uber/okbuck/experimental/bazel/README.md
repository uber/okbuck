# OkBazel - Experimental Bazel Support for OkBuck

[Bazel](https://bazel.build) is an [open
source](https://github.com/bazelbuild/bazel) build system made by Google for
fast, correct and reproducible builds. While Bazel supports many different
languages, OkBazel targets only server Java and Android applications.
Specifically, it transforms applications of the Gradle `java` and `android`
plugins into Bazel `java_library`, `java_binary`, `android_library` and
`android_binary` targets.

## Running OkBazel

First, ensure that the OkBuck jar is in the classpath of your Gradle
dependencies just as you would to run OkBuck. Then to apply the plugin, instead
of

    apply plugin: 'com.uber.okbuck'

put the following in your `build.gradle`:

    apply plugin: 'com.uber.okbazel'

Then to run OkBazel which will generate the Bazel `BUILD` files, from the root
of your Gradle project run

    ./gradlew okbazel

This will generate one `BUILD` file for each Gradle subproject, one `BUILD` file
in `okbazel/BUILD` and one `WORKSPACE` file. The `WORKSPACE` file contains an
`android_sdk_repository` rule which will contain the path of your Android SDK if
the `$ANDROID_HOME` environment variable is set. You will need to update
`WORKSPACE` to contain the `api_level` and `build_tools_version` from your SDK
that you wish to use.

## Setting up Bazel

Before using OkBazel, you will want to install Bazel on your system. See the
[Installing Bazel help
page](https://www.bazel.io/versions/master/docs/install.html) for more
information on installing the current version. For more information on using
Bazel, see the [Bazel User
Manual](https://www.bazel.io/versions/master/docs/bazel-user-manual.html).

## Testing

At this time, tests and annotation processors are not supported by OkBazel,
although Bazel does support them. For a description of how to run your tests
with Bazel, see the [Bazel Test
Encyclopedia](https://bazel.build/versions/master/docs/test-encyclopedia.html).

