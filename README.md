# OkBuck
[ ![Download](https://img.shields.io/bintray/v/piasy/maven/OkBuck.svg) ](https://bintray.com/piasy/maven/OkBuck/_latestVersion)
[![Master branch build status](https://travis-ci.org/Piasy/OkBuck.svg?branch=master)](https://travis-ci.org/Piasy/OkBuck)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-OkBuck-green.svg?style=flat)](https://android-arsenal.com/details/1/2593)

OkBuck is a gradle plugin, aiming to help developers utilize the super fast build system: BUCK, based on the existing project with Android Studio + gradle, and keep both build systems work, with few lines configuration.

[Wiki](https://github.com/Piasy/OkBuck/wiki), [中文版](README-zh.md)

## Why OkBuck?
Android Studio + Gradle has already been many Android developers' option, and to migrate to BUCK, there are many works to be done, which are difficult and buggy. OkBuck aims to provide a gradle plugin, which will do these buggy job for you automaticlly after few lines configuration.

Further more, you can still use OkBuck to maintain your BUCK build system when your gradle configurations changes, OkBuck let you even needn't write one line of the magic BUCK script! 

## Who is using OkBuck?
Logo | User | Repo
--- | --- | ---
![YOLO logo](https://frontend-yoloyolo-tv.alikunlun.com/official/v3/img/pc/logo.png) | [YOLO](https://www.yoloyolo.tv/) | -
![Piasy avatar](https://avatars2.githubusercontent.com/u/3098704?v=3&s=64) | [Piasy](https://github.com/Piasy) | [AndroidTDDBootStrap](https://github.com/Piasy/AndroidTDDBootStrap)

If you are using OkBuck in your project, [send me a e-mail](mailto:xz4215@gmail.com), I'll add your repo in this list.

## Full example
Configurations in root project `build.gradle` file:

```gradle
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.github.piasy:okbuck-gradle-plugin:1.0.0-beta3'
    }
}

allprojects {
    repositories {
        jcenter()
    }
}

apply plugin: 'com.github.piasy.okbuck-gradle-plugin'

okbuck {
    buildToolVersion "23.0.1"
    target "android-23"
    overwrite true
    checkDepConflict true
    resPackages = [
            dummylibrary: 'com.github.piasy.okbuck.example.dummylibrary',
            app         : 'com.github.piasy.okbuck.example',
            anotherapp  : 'com.github.piasy.okbuck.example.anotherapp',
            common      : 'com.github.piasy.okbuck.example.common',
            emptylibrary: 'com.github.piasy.okbuck.example.empty',
    ]
    linearAllocHardLimit = [
            app: 7194304
    ]
    primaryDexPatterns = [
            app: [
                    '^com/github/piasy/okbuck/example/AppShell^',
                    '^com/github/piasy/okbuck/example/BuildConfig^',
                    '^android/support/multidex/',
                    '^com/facebook/buck/android/support/exopackage/',
                    '^com/github/promeg/xlog_android/lib/XLogConfig^',
                    '^com/squareup/leakcanary/LeakCanary^',
            ]
    ]
    exopackage = [
            app: true
    ]
    appClassSource = [
            app: 'src/main/java/com/github/piasy/okbuck/example/AppShell.java'
    ]
    appLibDependencies = [
            app: [
                    'buck-android-support',
                    'multidex',
                    'javalibrary',
            ]
    ]
    flavorFilter = [
            app: [
                    'dev',
                    'prod',
            ]
    ]
}
```

## Explanations
+  OkBuck hosts on jcenter, so `jcenter()` must be added into `buildscript` and `allprojects`' `repositories` list, and must add before `apply plugin`
+  `buildToolVersion` specifies the version of the Android SDK Build-tools, default `23.0.1`
+  `target` specifies the Android target sdk version, which could be abtained by `<sdk home>/tools/android list targets --compact`, default `android-23`
+  `overwrite` is used to control whether overwrite existing BUCK files, default `false`
+  `checkDepConflict` is used to control whether check dependency conflict, default `true`
+  `resPackages` is a map, used for specifing Android library module and Android Application module's package name for generated resources, its key is module's name, value is package name, must be specified, it should be the same as `package` part in corresponding `AndroidManifest.xml` file
+  `linearAllocHardLimit` and `primaryDexPatterns` are maps, used for specifing linearAllocHardLimit and primaryDexPatterns used by BUCK multidex, more details about multidex configuration, please read [multidex wiki page](https://github.com/Piasy/OkBuck/wiki/Multidex-Configuration-Guide), if you don't need multidex (not enable it in `build.gradle`), you can ignore these two parameters
+  `exopackage`, `appClassSource` and `appLibDependencies` are maps, used for configuring BUCK exopackage mode, more details about exopackage configuration, please read [exopackage wiki page](https://github.com/Piasy/OkBuck/wiki/Exopackage-Configuration-Guide), if you don't need exopackage, you can ignore these three parameters
+  `flavorFilter` is a map, used for controlling OkBuck only generate BUCK files for flavors you need, default is empty, which will generate all flavors BUCK file
+  After apply OkBuck plugin (sync project after modify root project `build.gradle` file), two tasks are added into your gradle project , `okbuck`, and `okbuckClean`
  +  `okbuck` will generate all flavor (you specified) and variant BUCK configs, and each flavor + variant combination will have an BUCK alias
  +  `okbuckClean` will **delete all** files/dirs generated by OkBuck and BUCK
+  After execute `./gradlew okbuck`, there will be a `.buckconfig` file in your project root dir, inside it, there are lots of BUCK alias, e.g. `appDevDebug`, `appProdRelease`, `anotherappDebug` etc, which you could use to run BUCK build, e.g. `buck build appDevDebug` etc

## Troubleshooting
If you come with bugs of OkBuck, please [open an issue](https://github.com/Piasy/OkBuck/issues/new), and it's really appreciated to post the output of `./gradle okbuck --stacktrace --debug` at the same time, or you can join our BUCK QQ group: `170102067`.

## After apply OkBuck successfully
OkBuck can only generate BUCK configs for you, so if your source code is incompatible with BUCK, you need do more jobs.

Read more on [Known caveats wiki page](https://github.com/Piasy/OkBuck/wiki/Known-caveats). 

Maybe there are more caveats waiting for you, but for the super fast build brought by BUCK, it's worthwhile.

The rest modules in this repo is a full example usage of OkBuck and BUCK.

## Compatibility
OkBuck is tested under `gradle` 2.2.1 ~ 2.10, and `com.android.tools.build:gradle` 1.5.0. Other versions will not considered compatible, if you have compatibility problem, please fire an issue.

Exceptions:

+  gradle 2.4 need force jdk version to 1.7, [ref1](http://stackoverflow.com/a/21212790/3077508) and [ref2](http://stackoverflow.com/a/18144853/3077508)

## Contribution
Any form of contributions are welcome! See the [detail todo list](https://github.com/Piasy/OkBuck/wiki/TODO-list).

Note that you need create an empty file named `bintray.properties` in `/buildSrc/`, which is used for publishing to bintray.

## Acknowledgement
+  Thanks for Facebook open source [buck](https://github.com/facebook/buck) build system.
+  Thanks for the discussion and help from [promeG](https://github.com/promeG/) during my development.
+  Thanks for [ヤ①個亼簡單](#) of the manifest merge contribution, and the idea of multi-product flavor support.
+  Thanks for [hujin1860@gmail.com](mailto:hujin1860@gmail.com) of the manifest genrule advice.

## [Full Change log](CHANGELOG.md)

## Liscense
```
The MIT License (MIT)

Copyright (c) 2015 Piasy

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
