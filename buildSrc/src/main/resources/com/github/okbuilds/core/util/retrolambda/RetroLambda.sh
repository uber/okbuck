#!/bin/bash

DEPS=$(JARS=(`find gen-dir -name *.jar`); IFS=:; echo "${JARS[*]}")
java -Dretrolambda.inputDir=$1 -Dretrolambda.classpath=$1:android-jar:$DEPS -jar retrolambda-jar
