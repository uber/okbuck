# OkBuck

## 基本配置
工程根目录的`build.gradle`文件中加入配置：

```gradle
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.github.okbuilds:okbuild-gradle-plugin:0.5.0'
    }
}

apply plugin: 'com.github.okbuilds.okbuck-gradle-plugin'
```

大部分情况下, 上述配置就完成了。OkBuck托管在jcenter，所以 `jcenter()` 
必须加入到 `buildscript` 和 `allprojects` 的 `repositories` 列表中，
而且必须在 `apply plugin` 部分之前。

应用 OkBuck 插件之后，工程内将会产生两个 gradle task，`okbuck` 和 `okbuckClean`

+  `okbuck` 将会生成 BUCK 配置文件
+  `okbuckClean` 将会删除所有的 OkBuck 临时文件，BUCK 配置文件，以及 BUCK 临时文件

可以执行 `buck targets` 命令查看所有可以 build 的目标, 而生成的 `.buckconfig` 
文件中会指定多个 alias, 例如 `appDevDebug`，`appProdRelease`，`another-appDebug`
等，根据它们可以确定 BUCK build 的命令，例如 `buck build appDevDebug` 等。

## 自定义配置
```gradle
okbuck {
    buildToolVersion "23.0.3"
    target "android-23"
    linearAllocHardLimit = [
            app: 7194304
    ]
    primaryDexPatterns = [
            app: [
                    '^com/github/okbuilds/okbuck/example/AppShell^',
                    '^com/github/okbuilds/okbuck/example/BuildConfig^',
                    '^android/support/multidex/',
                    '^com/facebook/buck/android/support/exopackage/',
                    '^com/github/promeg/xlog_android/lib/XLogConfig^',
                    '^com/squareup/leakcanary/LeakCanary^',
            ]
    ]
    exopackage = [
            appDebug: true
    ]
    annotationProcessors = [
            "local-apt-dependency": ['com.okuilds.apt.ExampleProcessor']
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
            ],
            'appDemo': [
                    'buck-android-support',
                    'com.android.support:multidex',
                    'libraries/javalibrary:main',
                    'libraries/common:paidRelease',
            ]
    ]
    buckProjects = project.subprojects
    keep = []
}
```

## 详细解释
+  `buildToolVersion`指定Android SDK Build-tools版本，默认为`23.0.3`
+  `target`指定Android target sdk版本，可以运行`<sdk home>/tools/android list targets --compact`
获得，默认为`android-23`
+  `linearAllocHardLimit`和`primaryDexPatterns`都是map，用来配置BUCK multidex的
linearAllocHardLimit和primaryDexPatterns部分，更多详细关于multidex配置的说明，请参阅
[multidex wiki](https://github.com/OkBuilds/OkBuck/wiki/Multidex-Configuration-Guide)，
如果未使用multidex（未在`build.gradle`文件中开启），可以忽略这两个参数
+  `exopackage`和`appLibDependencies`都是map，用来配置BUCK exopackage，
更多详细关于exopackage配置的说明，请参阅[exopackage wiki](https://github.com/OkBuilds/OkBuck/wiki/Exopackage-Configuration-Guide)，
如果未使用exopackage，可以忽略这三个参数
+ `annotationProcessors` 用来声明项目中的注解处理器, key 为 module 路径, value 为注解处理器类的全名。
+  `buckProjects` 用于控制哪些 module 将使用 BUCK 进行构建, 默认是项目中的所有 module
+ 上述配置 map 的 key, 可以按照以下规则设置:  
 - 指定 module 名字, 就能为所有的 flavor 以及 build type 设置, 例如: `app`
 - 指定 module 名字以及 flavor 名字, 就能为指定 flavor 的所有 build type 设置, 例如: 'appDemo'
 - 指定 module 名字以及 build type 的名字, 就能为指定 build type 的所有 flavor 设置, 例如: 'appDebug'
 - 指定 module 名字, flavor 名字以及 build type 的名字, 例如: 'appDemoRelease'

## Troubleshooting
如果遇到任何问题，请[提一个issue](https://github.com/OkBuilds/OkBuck/issues/new)，如果能提供
`./gradle okbuck --stacktrace --debug`的输出，就是极好的了。如有任何OkBuck或者BUCK的使用问题，
欢迎加入**OkBuck使用问题交流群**：`170102067`。
