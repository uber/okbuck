#!/bin/sh
git clone https://github.com/facebook/buck.git && cd buck && ant && ./buck/buck --help