# Usage

## Configuring OkBuck
Example configuration
```gradle
okbuck {
    buildToolVersion = "24.0.2"
    target = "android-24"
    linearAllocHardLimit = [
            app: 16 * 1024 * 1024
    ]
    primaryDexPatterns = [
            app: [
                    '^com/uber/okbuck/example/AppShell^',
                    '^com/uber/okbuck/example/BuildConfig^',
                    '^android/support/multidex/',
                    '^com/facebook/buck/android/support/exopackage/',
                    '^com/github/promeg/xlog_android/lib/XLogConfig^',
                    '^com/squareup/leakcanary/LeakCanary^',
            ]
    ]
    exopackage = [
            appDebug: true
    ]
    appLibDependencies = [
            'appProd': [
                    'buck-android-support',
                    'com.android.support:multidex',
                    'libraries/javalibrary:main',
                    'libraries/common:paidRelease',
            ],
            'appDev': [
                    'buck-android-support',
                    'com.android.support:multidex',
                    'libraries/javalibrary:main',
                    'libraries/common:freeDebug',
            ]
    ]
    annotationProcessors = [
            "local-apt-dependency": ['com.okbuck.apt.ExampleProcessor']
    ]
    buckProjects = project.subprojects
    extraBuckOpts = [
        'appDebug': [
            "android_binary": ["trim_resource_ids = True"]
        ]
    ]

    wrapper {
        repo = 'https://github.com/facebook/buck.git'
    }

    transform {
        transforms = [
                'appDebug' : [
                        [transform : "<FULL_QUALIFIED_CLASS_NAME_FOR_TRANSFORM_CLASS>",
                         configFile : "<OPTIONAL_CONFIGURATION_FILE>"]
                ],
        ]
    }

    experimental {
        transform = true
    }
}

dependencies {
    transform "<TRANSFORM_DEPENDENCIES>"
}
```

+  `buildToolVersion` specifies the version of the Android SDK Build-tools, defaults to `24.0.2`
+  `target` specifies the Android compile sdk version, default is `android-24`
+  `linearAllocHardLimit` and `primaryDexPatterns` are used to configure options used by buck for multidex apps. For more details about multidex configuration, please read the
[Multidex wiki](https://github.com/uber/okbuck/wiki/Multidex-Configuration-Guide).
+  `exopackage` and `appLibDependencies` are used for
configuring buck's exopackage mode. For more details about exopackage configuration,
please read the [Exopackage wiki](https://github.com/uber/okbuck/wiki/Exopackage-Configuration-Guide), if you don't need exopackage, you can ignore these parameters
+ `annotationProcessors` is used to depend on annotation processors declared locally as another gradle module in the same project.
+  `buckProjects` is a set of projects to generate buck files for. Default is all sub projects.
+  `extraBuckOpts` provides a hook to add additional configuration options for buck [android_binary](https://buckbuild.com/rule/android_binary.html) rules
+  `wrapper` is used to configure creation of the buck wrapper script.
 - `repo` - The git url of any custom buck fork. Default is none.
+ The keys used to configure various options can be for
 - All buildTypes and flavors i.e `app`
 - All buildTypes of a particular flavor i.e 'appDemo'
 - All flavors of a particular buildType i.e 'appDebug'
 - A particular variant (buildType + flavor combination) i,e 'appDemoRelease'

## Configuring Buck

You can configure behavior of buck by using various configuration files like [buckconfig](https://buckbuild.com/concept/buckconfig.html), [buckjavaargs](https://buckbuild.com/concept/buckjavaargs.html), [bucklogging](https://buckbuild.com/contributing/logging.html) and [buckversion](https://buckbuild.com/concept/buckversion.html). It is recommended to atleast set the buckversion so you are pointed at a good revision of the [buck
codebase](https://github.com/facebook/buck).

Similarly, watchman can be configured for better performance via [watchmanconfig](https://facebook.github.io/watchman/docs/config.html). For examples of these configuration files, you can look at the existing configuration files in this repository.

The `buckw` and various configuration files can be checked into version control. The following paths can be ignored `buck-out .buckd .okbuck .buckconfig.local **/BUCK`

You can type `./buckw targets` to get a list of targets that can be built. The generated `.buckconfig.local` file will have some aliases setup to build your apps without having to type the rulename. i.e you can build targets via the alias like `./buckw build appDebug another-appPaidRelease` etc.
