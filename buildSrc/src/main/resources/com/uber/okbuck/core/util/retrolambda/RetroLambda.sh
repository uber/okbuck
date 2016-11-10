#!/bin/bash
java -Dretrolambda.inputDir=$1 -Dretrolambda.classpath=$1:`cat $DEPENDENCIES_CLASSPATH_FILE` -jar retrolambda-jar
