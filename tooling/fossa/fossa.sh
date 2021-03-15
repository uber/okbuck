#!/bin/bash
set -exo pipefail

TEMP_DIR=$(mktemp -d)
curl -H 'Cache-Control: no-cache' https://raw.githubusercontent.com/fossas/fossa-cli/master/install.sh | bash -s -- -b "$TEMP_DIR"

"$TEMP_DIR/fossa" analyze
