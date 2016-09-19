# OkBuck
[![Master branch build status](https://travis-ci.org/OkBuilds/OkBuck.svg?branch=master)](https://travis-ci.org/OkBuilds/OkBuck)
[ ![Download](https://api.bintray.com/packages/okbuild/maven/OkBuild/images/download.svg) ](https://bintray.com/okbuild/maven/OkBuild/_latestVersion)

OkBuck is a gradle plugin that lets developers utilize the [Buck](https://buckbuild.com/) build system on a gradle project.

[Wiki](https://github.com/OkBuilds/OkBuck/wiki), [中文版](https://github.com/OkBuilds/OkBuck/blob/master/README-zh.md)

## Installation
### Mac OS X
```bash
brew update

# Required to build and use buck
brew install android-ndk ant

# Optional, but recommended for faster development
brew install watchman
```

### Linux
Installation instrcutions for: [Android NDK](https://developer.android.com/ndk/downloads/index.html), [Ant](http://ant.apache.org/), [Watchman](https://facebook.github.io/watchman/docs/install.html)

### Configuration
Set the `ANDROID_NDK` environment variable to point to your android ndk installation

In root project `build.gradle` file:

```gradle
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.github.okbuilds:okbuild-gradle-plugin:0.5.3'
    }
}

apply plugin: 'com.github.okbuilds.okbuck-gradle-plugin'
```

After applying the plugin, the following tasks will be added to the root project
  +  `buckWrapper` will create a buck wrapper script and varios configuration files to invoke buck commands
  +  `okbuck` will generate BUCK files

### Buck Wrapper

Run the buck wrapper task
```bash
./gradlew :buckWrapper
```
This creates a `buckw` wrapper similar to the gradle wrapper. It also creates various configuration files like [buckconfig](https://buckbuild.com/concept/buckconfig.html), [buckjavaargs](https://buckbuild.com/concept/buckjavaargs.html), [bucklogging](https://buckbuild.com/contributing/logging.html), [buckversion](https://buckbuild.com/concept/buckversion.html) and [watchmanconfig](https://facebook.github.io/watchman/docs/config.html)

Invoking buck comamnds via the buck wrapper has several advantages
- Downloads/installs/updates buck
- Minimal overhead to decide when to run `okbuck` before invoking buck (using watchman)
- Handles gracefully, the cases when `okbuck` task fails/is stopped abruptly

Please make sure you have watchman installed for `buckw` to run `okbuck` only when needed. A new `okbuck` run is typically needed when gradle configuration or Android manifest files change. If watchman is not installed, the `okbuck` task in always run before invoking any buck commands.

The `buckw` and various configuration files can be checked into version control. The following paths can be ignored `buck-out .buckd .okbuck .buckconfig.local **/BUCK`

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

See the [Usage](https://github.com/OkBuilds/OkBuck/blob/master/Usage.md) page for complete details on how to configure the plugin.

## Liscense
```
The MIT License (MIT)

Copyright (c) 2015 OkBuilds

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
