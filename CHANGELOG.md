# Changelog

### Version 0.53.1
* Add support for exporting raw dependencies from okbuck

### Version 0.53.2
* Bug fix to handle export dependencies file path correctly.

### Version 0.53.3
* Add configuration cleanCacheDir to conditionally delete the cache directory or
  just the existing dependency rules files

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

### Version 0.54.4-UBER-bazel-migration-5
* Added support for Kotlin 2.1.20
