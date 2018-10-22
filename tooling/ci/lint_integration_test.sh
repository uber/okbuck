#!/bin/bash

./buckw build //libraries/lintErrorLibrary:lint_debug -v 0
if [ $? -eq 0 ]; then
    echo "Lint passed when it should have failed. Please revisit any changes you may have made to lint wiring.";
    exit 1;
else
    success=y
    testfile='buck-out/gen/libraries/lintErrorLibrary/lint_debug/lint_debug_out/lint-results.xml'
    for check in 'id=\"AndroidColorDetector\"' 'id=\"UnusedResources\"' 'id=\"MissingApplicationIcon\"' 'id=\"NewApi\"' 'id=\"DontUseSystemTime\"' 'id=\"InvalidR2Usage\"'
    do
    if grep -q "$check" "$testfile"
    then :
    else
        printf 'no match for %s\n' "$check"
        success=n
    fi
    done
    if [ "$success" = "y" ]; then
        echo "Lint failed as expected."
    else
        echo "Lint did not create the expected output. Please double check your changes to resolve.";
        exit 1;
    fi
fi
