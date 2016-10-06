#!/bin/bash

java -Dretrolambda.inputDir=$1 -Dretrolambda.classpath=$1:$ANDROID_JAR:$DEPS -jar retrolambda-jar
