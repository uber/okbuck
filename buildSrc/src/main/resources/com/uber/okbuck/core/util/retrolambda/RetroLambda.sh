#!/bin/bash
DEPENDENCIES_CLASSPATH=("`cat ""$DEPENDENCIES_CLASSPATH_FILE""`")
java -Dretrolambda.inputDir=$1 -Dretrolambda.classpath=$1:$DEPENDENCIES_CLASSPATH -jar retrolambda-jar
