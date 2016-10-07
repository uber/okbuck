#!/bin/bash

java -Dretrolambda.inputDir=$1 -Dretrolambda.classpath=$1:$DEPS -jar retrolambda-jar
