# Change log
+  1.0.0
  +  **refactor round 3**, better implementation, better code
  +  fix bug about too long filename, [#60](https://github.com/Piasy/OkBuck/issues/60)
  +  add flavorFilter
  +  add aidl support, [#40](https://github.com/Piasy/OkBuck/issues/40)
+  0.4.11: fix bug about aar dependency inside local libs dir
+  0.4.10: fix bug about sign config, [#56](https://github.com/Piasy/OkBuck/issues/56)
+  0.4.9: fix bug in exopackage
+  0.4.8: minimal support for exopackage
+  0.4.7: add multidex support, see more at [wiki page Multidex support](https://github.com/Piasy/OkBuck/wiki/Multidex-support)
+  0.4.6: solve problems when generate `AndroidManifest.xml` like below:
  
  ```bash
  [AndroidManifest.xml:7, AndroidManifest.xml:3] Trying to merge incompatible /manifest/uses-permission[@name=android.permission.ACCESS_WIFI_STATE] element:
    <uses-permission android:name=android.permission.ACCESS_WIFI_STATE>
  --    @android:name = android.permission.ACCESS_WIFI_STATE
  ++    @ns0:name = android.permission.ACCESS_WIFI_STATE
  ```

+  0.4.5: use `genrule` to generate `AndroidManifest.xml`
+  0.4.4: fix [#42](https://github.com/Piasy/OkBuck/issues/42)
+  0.4.2: different libraries may have the same name, so turn `checkDependencyDiffersByVersion` to be optional temporarily
+  0.4.1: naive way to fix problems in `checkDependencyDiffersByVersion`
+  0.4.0:
  +  multi-product flavor support! although need improve it.
  +  fix [#7](https://github.com/Piasy/OkBuck/issues/7)
  +  fix [#8](https://github.com/Piasy/OkBuck/issues/8)
  +  fix [#29](https://github.com/Piasy/OkBuck/issues/29)
  +  fix [#30](https://github.com/Piasy/OkBuck/issues/30)
+  0.3.5: fix [#39](https://github.com/Piasy/OkBuck/issues/39)
+  0.3.4: fix [#22](https://github.com/Piasy/OkBuck/issues/22) prebuilt native library support
+  0.3.3:
  +  fix [#25](https://github.com/Piasy/OkBuck/issues/25), [#27](https://github.com/Piasy/OkBuck/issues/27)
  +  merge [#35](https://github.com/Piasy/OkBuck/pull/35)
+  0.3.2: better error message
+  0.3.1: fix [#31](https://github.com/Piasy/OkBuck/issues/31)
+  0.3.0:
  +  fix [#15](https://github.com/Piasy/OkBuck/issues/15) again.
  +  better dependency process, partly fix [Known caveats: handle dependency conflict](https://github.com/Piasy/OkBuck/wiki/Known-caveats#handle-dependency-conflict)
  +  **refactor round 2**, ready for your contributions!
+  0.2.7:
  +  fix overwrite doesn't work bug
  +  fix bug when Android library module doesn't have res dir
  +  .buckconfig will ignore .svn dirs
  +  fix [#6](https://github.com/Piasy/OkBuck/issues/6) again...
+  0.2.6: fix [#14](https://github.com/Piasy/OkBuck/issues/14)
+  0.2.5: fix [#15](https://github.com/Piasy/OkBuck/issues/15)
+  0.2.4: fix [#12](https://github.com/Piasy/OkBuck/issues/12)
+  0.2.3: fix [#11](https://github.com/Piasy/OkBuck/issues/11)
+  0.2.2: fix [#6](https://github.com/Piasy/OkBuck/issues/6)
+  [v0.2.1](https://github.com/Piasy/OkBuck/releases/tag/v0.2.1)
+  [v0.1.0](https://github.com/Piasy/OkBuck/releases/tag/v0.1.0)
+  [v0.0.1](https://github.com/Piasy/OkBuck/releases/tag/v0.0.1)
