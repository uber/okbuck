#!/bin/bash

if [ -z "$BUILD_CMD" ]; then
	echo "BUILD_CMD is not set"
	exit 1
fi

SKIP_OKBUCK= ./buckw --version
export RESOLVED_KOTLIN_HOME=$(./buckw build //.okbuck/workspace/kotlin_home:kotlin_home --show-output | awk '{print $2}')
sed -i.bak "s://.okbuck/workspace/kotlin_home\:kotlin_home:${RESOLVED_KOTLIN_HOME}:g" .okbuck/config/okbuck.buckconfig

echo "Running BUILD_CMD: $BUILD_CMD"

if [ "$BUILD_CMD" = "build" ]; then
	./buckw targets //... --type android_binary android_instrumentation_apk java_test groovy_test robolectric_test kotlin_test scala_test | xargs ./buckw build
elif [ "$BUILD_CMD" = "lint" ]; then
	./buckw targets //... --type genrule | grep -v lintErrorLibrary | xargs ./buckw build && ./tooling/ci/lint_integration_test.sh
elif [ "$BUILD_CMD" = "test" ]; then
	./buckw test //... --include unit --always_exclude
else
	echo "unrecognized BUILD_CMD: $BUILD_CMD"
	exit 1
fi
