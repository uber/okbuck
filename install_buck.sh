#!/bin/sh
sudo add-apt-repository ppa:fkrull/deadsnakes -y && sudo apt-get update -qq && sudo apt-get install -y python2.7 && git clone https://github.com/facebook/buck.git && cd buck && ant