# Changelog

### Version 0.53.1
* Add support for exporting raw dependencies from okbuck

### Version 0.53.2
* Bug fix to handle export dependencies file path correctly.

### Version 0.53.3
* Add configuration cleanCacheDir to conditionally delete the cache directory or just the existing dependency rules files

### Version 0.54.0
* Stop using /tmp for Android Lint gen-rule and use buck-out tmp instead

### Version 0.54.1
* Clean up leftover tmp file for Android Lint genrule

### Version 0.54.2
* Signing certificate is invalid, replacement release with new signature.
* No other code changes

### Version 0.54.3
* Added support for Android API 32, 33, and 34
* Bump robolectric to 4.8.2
* Bump pre-instrumented JARs to i4

### Version 0.54.4
* Added support for using Android Lint 31.3+

### Version 0.54.5
* Added `labelsMap` configuration to `externalDependencies` block for adding custom labels to prebuilt dependency rules
* Migrated Robolectric's deprecated code to recommended alternatives:
  - Added `:libraries:robolectric-base` to Gradle modules
  - Added missing jUnit dependency to robolectric-base
  - Replaced deprecated `getAppManifest()` with `getManifestFactory()` and created `BuckManifestFactory`
* Updated GitHub Actions workflows:
  - Updated runner image to `ubuntu-24.04` (ubuntu-20.04 is deprecated)
  - Updated `actions/checkout` to v4
  - Updated `actions/setup-java` to v4 with temurin distribution
  - Removed rxPermissions and XLog dependencies
  - Updated to Python 3.8
* Added support for Kotlin 2.2.0
* Upgraded to Gradle 7.6
* **Breaking change for contributors**: Java 11+ is now required to build the plugin (plugin runtime still targets Java 8)
* Upgraded Rocker plugin from 1.3.0 to 2.2.1
* Upgraded gradle-maven-publish-plugin from 0.18.0 to 0.27.0
