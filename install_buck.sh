#!/bin/sh
git clone --depth 1 https://github.com/OkBuilds/buck.git && cd buck && git checkout okbuck && ant
