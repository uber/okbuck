# OkBuck

## 基本配置
工程根目录的`build.gradle`文件中加入配置：

```gradle
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.github.piasy:okbuck-gradle-plugin:1.0.0-beta4'
    }
}

allprojects {
    repositories {
        jcenter()
    }
}

apply plugin: 'com.github.piasy.okbuck-gradle-plugin'

okbuck {
    resPackages = [
            dummylibrary: 'com.github.piasy.okbuck.example.dummylibrary',
            app         : 'com.github.piasy.okbuck.example',
            anotherapp  : 'com.github.piasy.okbuck.example.anotherapp',
            common      : 'com.github.piasy.okbuck.example.common',
            emptylibrary: 'com.github.piasy.okbuck.example.empty',
    ]
}
```

## 解释
+  OkBuck托管在jcenter，所以`jcenter()`必须加入到`buildscript`和`allprojects`的
`repositories`列表中，而且必须在`apply plugin`部分之前
+  `resPackages`是一个map，用来指定每个module生成的的资源文件的包名，key是module的名字，
value是指定的包名，通常和该module的`AndroidManifest.xml`中的`package`配置保持一致

## 完整示例
```gradle
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.github.piasy:okbuck-gradle-plugin:1.0.0-beta4'
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

## 详细解释
+  `buildToolVersion`指定Android SDK Build-tools版本，默认为`23.0.1`
+  `target`指定Android target sdk版本，可以运行`<sdk home>/tools/android list targets --compact`
获得，默认为`android-23`
+  `overwrite`配置是否覆盖已有的BUCK配置文件，默认为`false`
+  `checkDepConflict`指定是否检查并警告依赖冲突，默认为`true`
+  `linearAllocHardLimit`和`primaryDexPatterns`都是map，用来配置BUCK multidex的
linearAllocHardLimit和primaryDexPatterns部分，更多详细关于multidex配置的说明，请参阅
[multidex wiki](https://github.com/Piasy/OkBuck/wiki/Multidex-Configuration-Guide)，
如果未使用multidex（未在`build.gradle`文件中开启），可以忽略这两个参数
+  `exopackage`，`appClassSource`和`appLibDependencies`都是map，用来配置BUCK exopackage，
更多详细关于exopackage配置的说明，请参阅[exopackage wiki](https://github.com/Piasy/OkBuck/
wiki/Exopackage-Configuration-Guide)，如果未使用exopackage，可以忽略这三个参数
+  `flavorFilter`是一个map，用来控制只生成自己想要的flavor的BUCK配置，默认为空，表示生成所有flavor的BUCK配置
+  应用OkBuck插件之后，工程内将会产生两个gradle task，`okbuck`和`okbuckClean`
  +  `okbuck`将会生成BUCK配置文件，包括指定的所有flavor的配置
  +  `okbuckClean`将会删除所有的OkBuck临时文件，BUCK配置文件，以及BUCK临时文件
+  成功执行`./gradlew okbuck`后，工程根目录将生成一个`.buckconfig`文件，其中定义了多个BUCK alias，
例如`appDevDebug`，`appProdRelease`，`anotherappDebug`等，根据它们可以确定BUCK build的命令，
例如`buck build appDevDebug`等

## Troubleshooting
如果遇到任何问题，请[提一个issue](https://github.com/Piasy/OkBuck/issues/new)，如果能提供
`./gradle okbuck --stacktrace --debug`的输出，就是极好的了。如有任何OkBuck或者BUCK的使用问题，
欢迎加入**OkBuck使用问题交流群**：`170102067`。
