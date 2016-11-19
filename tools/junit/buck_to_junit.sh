#!/bin/bash

# Get script directory
prog="$0"
while [ -h "${prog}" ]; do
    newProg=`/bin/ls -ld "${prog}"`
    newProg=`expr "${newProg}" : ".* -> \(.*\)$"`
    if expr "x${newProg}" : 'x/' >/dev/null; then
        prog="${newProg}"
    else
        progdir=`dirname "${prog}"`
        prog="${progdir}/${newProg}"
    fi
done
DIR=`dirname $prog`

saxonJar="$DIR/Saxon-HE-9.7.0-11.jar"
buckToJunitXsl="$DIR/buckToJunit.xsl"

# Perform conversion
java -jar $saxonJar -xsl:$buckToJunitXsl -s:$1 -o:$2
