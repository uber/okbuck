#!/bin/bash

if [ -z "$BUILD_CMD" ]; then
	echo "BUILD_CMD is not set"
	exit 1
fi

SKIP_OKBUCK= ./buckw --version

echo "Running BUILD_CMD: $BUILD_CMD"

if [ "$BUILD_CMD" = "build" ]; then
	./buckw targets //... --type android_binary android_instrumentation_apk java_binary | xargs ./buckw build
elif [ "$BUILD_CMD" = "lint" ]; then
	./buckw targets //... --type genrule | grep -v lintErrorLibrary | xargs ./buckw build && ./tooling/ci/lint_integration_test.sh
elif [ "$BUILD_CMD" = "test" ]; then
	./buckw test //... --include unit --always_exclude
else
	echo "unrecognized BUILD_CMD: $BUILD_CMD"
	exit 1
fi
