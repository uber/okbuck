#!/bin/sh

git clone -b okbuck --single-branch --depth 1 https://github.com/OkBuilds/buck.git && cd buck && ant
