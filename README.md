# OkBuck
[![Master branch build status](https://travis-ci.org/uber/okbuck.svg?branch=master)](https://travis-ci.org/uber/okbuck)
[ ![Download](https://api.bintray.com/packages/uber/gradle-plugins/okbuck/images/download.svg) ](https://bintray.com/uber/gradle-plugins/okbuck/_latestVersion)

OkBuck is a gradle plugin that lets developers utilize the [Buck](https://buckbuild.com/) build system on a gradle project.

[Wiki](https://github.com/uber/okbuck/wiki), [中文版](https://github.com/uber/okbuck/blob/master/README-zh.md)

## Installation
These are needed to build with buck
Installation instructions for: [Android NDK](https://developer.android.com/ndk/downloads/index.html), [Ant](http://ant.apache.org/), [Watchman](https://facebook.github.io/watchman/docs/install.html)

### Configuration
Set the `ANDROID_NDK` environment variable to point to your android ndk installation

In root project `build.gradle` file:

```gradle
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.uber:okbuck:0.20.9'
    }
}

repositories {
    jcenter()
}

apply plugin: 'com.uber.okbuck'
```

After applying the plugin, the following tasks will be added to the root project
  +  `buckWrapper` will create a buck wrapper script and various configuration files to invoke buck commands
  +  `okbuck` will generate BUCK files

### Buck Wrapper

Run the buck wrapper task
```bash
./gradlew :buckWrapper
```
This creates a `buckw` wrapper similar to the gradle wrapper

Invoking buck commands via the buck wrapper has several advantages
- Downloads/installs/updates buck
- Minimal overhead to decide when to run `okbuck` before invoking buck (using watchman)
- Handles gracefully, the cases when `okbuck` task fails/is stopped abruptly

Please make sure you have watchman installed for `buckw` to run `okbuck` only when needed. A new `okbuck` run is typically needed when gradle configuration or Android manifest files change. If watchman is not installed, the `okbuck` task in always run before invoking any buck commands.

## Usage

```bash
# List all buck targets
./buckw targets

# Build a target
./buckw build <target>

# Install an apk target
./buckw install --run <apk-target>

# Generate an Intellij project
./buckw project
```

See the [Usage](https://github.com/uber/okbuck/blob/master/Usage.md) page for complete details on how to configure OkBuck and Buck.

## Buck Http Cache

To speed up your builds even more, you can use an implementation of [Buck's HTTP Cache API](https://github.com/uber/buck-http-cache) to take advantage of building once and using the same build artifacts on all machines.

## Contributors

We'd love for you to contribute to our open source projects. Before we can accept your contributions, we kindly ask you to sign our [Uber Contributor License Agreement](https://docs.google.com/a/uber.com/forms/d/1pAwS_-dA1KhPlfxzYLBqK6rsSWwRwH95OCCZrcsY5rk/viewform).

- If you **find a bug**, open an issue or submit a fix via a pull request.
- If you **have a feature request**, open an issue or submit an implementation via a pull request
- If you **want to contribute**, submit a pull request.

## License
```
Copyright (c) 2016 Uber Technologies, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
