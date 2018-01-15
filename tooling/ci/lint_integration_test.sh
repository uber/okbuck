#!/bin/bash

./buckw build //libraries/lintErrorLibrary:lint_debug -v 0;
if [ $? -eq 0 ]; then
    echo "Lint passed when it should have failed. Please revisit any changes you may have made to lint wiring.";
    exit 1;
else
    if grep -q 'id=\"DontUseSystemTime\"' 'buck-out/gen/libraries/lintErrorLibrary/lint_debug/lint_debug_out/lint-results.xml'; then
    echo "Lint failed as expected.";
    else
    echo "Lint did not create the expected output. Please double check your changes to resolve.";
    exit 1;
    fi
fi
