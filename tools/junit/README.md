Convert buck xml test reports to Junit
======================================

Run tests
```
./buckw test --include unit --xml build/buck-test.xml
```

Perform Conversion
```
./tools/junit/buck_to_junit.sh build/buck-test.xml build/buck-junit.xml
```

`build/buck-junit.xml` will contain the test results in junit xml format
