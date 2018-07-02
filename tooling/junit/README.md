Convert buck xml test reports to Junit
======================================

Run tests
```
./buckw test //... --include unit --always_exclude --xml build/buck-test.xml
```

Perform Conversion
```
./tooling/junit/buck_to_junit.sh build/buck-test.xml build/buck-junit.xml
```

`build/buck-junit.xml` will contain the test results in junit xml format
