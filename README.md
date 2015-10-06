# OkBuck
[ ![Download](https://api.bintray.com/packages/piasy/maven/OkBuck/images/download.svg) ](https://bintray.com/piasy/maven/OkBuck/_latestVersion)
[![Master branch build status](https://travis-ci.org/Piasy/OkBuck.svg?branch=master)](https://travis-ci.org/Piasy/OkBuck)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-OkBuck-green.svg?style=flat)](https://android-arsenal.com/details/1/2593)

A gradle plugin that helps developer migrate to code with Android Studio + Gradle, but build &amp;&amp; install with buck.
 	
Only ~~Twelve~~ **Ten** line config to migrate from Android Studio + gradle to facebook BUCK.

[中文版](README-zh.md)

## Why OkBuck?
Android Studio + Gradle has been many Android developers' option, and to migrate to buck, there are many works to be done, both difficult and buggy. OkBuck aims to provide a gradle plugin, which will do these buggy job for you automaticlly after several lines configuration.

Further more, you can still use OkBuck to maintain your BUCK build system when your gradle configurations changes, OkBuck let you even needn't know how to write the magic BUCK script! 

## What will OkBuck do for you?
+  Generate `.buckconfig`.
+  Copy third-party libraries' jar/aar files to your rootProject directory.
+  Generate `BUCK` file for third-party libraries and each sub modules.
+  After you run `./gradlew okbuck` in your rootProject root directory, you can run `buck install app` now (suppose your application module names `app`).

## How to use OkBuck?
1. Add this lines into buildscript dependencies part of root rootProject build.gradle: `classpath "com.github.piasy:okbuck-gradle-plugin:${latest version}"`

2. Add this line into root rootProject build.gradle: `apply plugin: 'com.github.piasy.okbuck-gradle-plugin'`

3. Add this `okbuck` block into root rootProject build.gradle:
    
    ```gradle
    okbuck {
        target "android-23"
        overwrite true
        resPackages = [
            dummylibrary: 'com.github.piasy.okbuck.example.dummylibrary',
            app: 'com.github.piasy.okbuck.example',
        ]
    }
    ```

    +  `android-23` works equally with `compileSdkVersion 23`; 
    +  ~~`debug.keystore` and `debug.keystore.properties` are signing related file, which should be put under **application module's root directory**~~
    +  no need to specify signing config anymore:
      +  if you have already set **only one** signing config in your build.gradle
      +  but you need to configure git to ignore your signing secrete, add this line to your **root rootProject .gitignore file**: `.okbuck/keystore`
      +  if you have multiple signing config, or you want put your signing config in another dir which **under your root rootProject dir**, you can set it like below, `keystoreDir` is used to config the path OkBuck to put your generated signing config (relative to your root rootProject dir, no prefix `/`), and `signConfigName` is to set the name of the signing config you want to use.
        ```gradle
            okbuck {
                target "android-23"
                keystoreDir ".okbuck/keystore"
                signConfigName "release"
                overwrite true
                resPackages = [
                    dummylibrary: 'com.github.piasy.okbuck.example.dummylibrary',
                    app: 'com.github.piasy.okbuck.example',
                    common: 'com.github.piasy.okbuck.example.common',
                ]
            }
        ```
        +  but also remember to configure git to ignore your signing secrete
        +  full example could be found in the app module of this repo, [root rootProject build.gradle](build.gradle), [app module build.gradle](app/build.gradle)
    +  `overwrite` is used to control whether overwrite existing buck files; 
    +  `resPackages` is used to set Android library module and Android Application module's package name for generated resources (`R` in common cases), you need substitute dummylibrary/app with your own module name, and set the corrosponding package name inside the single quote, which should be the same package name specified in the corrosponding module's AndroidManifest.xml.
    
4. After executing the okbuck gradle task by `./gradlew okbuck`, you can run `buck install app` now, enjoy your life with buck :)
    +  there are 3 tasks added into your gradle project after you add `apply plugin: 'com.github.piasy.okbuck-gradle-plugin'`, `okbuck`, `okbuckDebug`, and `okbuckRelease`
    +  `okbuck` is the shortcut for `okbuckRelease`
    +  `okbuckDebug` and `okbuckRelease` will make your `debugCompile` and `releaseCompile` dependencies visible in buck's build, including annotation processor

5. It's really only ~~12~~ **10** lines config: ~~step one will only need one line `classpath 'com.github.piasy:okbuck-gradle-plugin:0.0.1'` when OkBuck is sync to jcenter (this will be soon)~~ available now, and then these three steps above only need ~~12~~ **10** lines!

## More job need to be done
OkBuck can only generate the buck config for you, so if your source code is incompitable with buck, you need do more job.

+  Dependency conflict:

    If you see `*** has already been defined` or
    
    ```java
    BUILD FAILED: //app:bin#dex_merge failed on step dx with an exception:
    Multiple dex files define***
    com.android.dex.DexException: Multiple dex files define ***
    ```
    
    when you run `buck install app`, you come with dependency confliction, which means multiple modules (let's name them moduleA and moduleB) depend on the same dependency (such as gson), and another module (name it moduleC) depends on moduleA and moduleB, disaster happens. Current work-around is to move all common dependencies into one module, such as moduleA, and make all modules depend on moduleA. This will be addressed in future releases of OkBuck.

+  Dependencies in local libs directory may not be imported correctly. Current work-around is to avoid use local dependencies, but this problem will be definitely addressed soon by OkBuck.
    
+  References to `R`. Buck is not compitable with libraries like ButterKnife, because R definitions generated by buck are not final, current work-around is change `@Bind`/`@InjectView` to `ButterKnife.findById`, and change `@OnClick` to `setOnClickListener`, etc. And reference to `R` resources from another module may also have problems, avoid doing this at this moment please, and also avoid defining resources in different module with the same resources name.

+  versionCode, versionName, targetSdkVersion, minSdkVersion should be set in AndroidManifest.xml rather in build.gradle, example:

    ```xml
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="com.github.piasy.okbuck.example"
          android:versionCode="1"
          android:versionName="1.0"
        >
    
        <uses-sdk
                android:targetSdkVersion="23"
                android:minSdkVersion="15"
                />
                ...
    </manifest>
    ```

+  `javax.annotation` dependency: if your module depends on `javax.annotation`, use the `compile` scope rather than `provided` scope.

+  buck can't compile with java 8, so it's incompitable with retrolambda, no more lambda :(
    
+  Maybe there are more caveats waiting for you, but for the super fast build brought by buck, it's worthwhile.

+  The rest modules in this repo is a full example usage of OkBuck.

## Known caveats
+  Not compitable with `ButterKnife` (buck)
+  Not compitable with `RetroLambda` (buck)
+  `javax.annotation` dependency should be `compile` scope, rather `provided` (OkBuck)
+  Cross module reference on `R` (buck), see above and avoid it
+  Could not refer to design support library's string resource `appbar_scrolling_view_behavior` (buck), that's the specific scenario of the above caveat, quick solution:
  +  define your own string resource: `<string name="my_appbar_scrolling_view_behavior" translatable="false">android.support.design.widget.AppBarLayout$ScrollingViewBehavior</string>`, and use it in your layout file
  +  or use the content directly in your layout file: `app:layout_behavior="android.support.design.widget.AppBarLayout$ScrollingViewBehavior"`
+  (buck & OkBuck) BUCK doesn't support debuggable apk generation easily, current quick solution is add `android:debuggable="true"` to your AndroidManifest.xml, OkBuck will fix this in near future.

## Troubleshooting
If you come with bugs of OkBuck, please [open an issue](https://github.com/Piasy/OkBuck/issues/new), and it's really appreciated to post the output of `./gradle okbuck` at the same time.

## TODO
+  ~~handle apt, provided dependencies~~
+  res reference on aar dependency
+  make BUCK's output apk support debug
+  ~~debugCompile/releaseCompile support~~
+  ~~build config~~ only under defaultConfig dsl will work, see below item
+  ~~product flavor support~~ it seems buck doesn't support multi-product flavors, [see](http://stackoverflow.com/a/26001029/3077508), or do I miss something?
+  test/androidTest support
+  proguard support
+  better solution for dependency conflict
+  better solution for local jar dependency
+  more configuration option
+  ~~ci~~
+  ~~code optimization/java doc~~

## Acknowledgement
+  Thanks for Facebook open source [buck](https://github.com/facebook/buck) build system.
+  Thanks for the discussion and help from [promeG](https://github.com/promeG/) during my development.
