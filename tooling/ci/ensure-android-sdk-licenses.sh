#!/usr/bin/env bash

export TOOLS_BIN_PATH="$ANDROID_HOME/tools/bin"
export SDKMANAGER_PATH="$TOOLS_BIN_PATH/sdkmanager"

if [ ! -e ${SDKMANAGER_PATH} ]; then
    echo "sdkmanager tool not found in '$SDKMANAGER_PATH', updating Android SDK tools..."

    android update sdk --no-ui --all --filter tools

    echo "Android SDK tools updated."

    yes | "$SDKMANAGER_PATH" --licenses
else
    echo "No need to update Android SDK tools, sdkmanager found in: $TOOLS_BIN_PATH/"
fi
