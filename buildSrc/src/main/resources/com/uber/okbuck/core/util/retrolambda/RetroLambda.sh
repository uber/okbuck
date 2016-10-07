#!/bin/bash

java -Dretrolambda.inputDir=$1 -Dretrolambda.classpath=$1:$DEPENDENCIES_CLASSPATH -jar retrolambda-jar
