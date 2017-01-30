#!/bin/bash
set -e

./gradlew transform-cli:shadowJar && cp ./transform-cli/build/libs/transform-cli.jar ./plugin/src/main/resources/com/uber/okbuck/core/util/transform/transform-cli.jar 
