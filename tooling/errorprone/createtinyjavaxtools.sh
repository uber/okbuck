#!/bin/bash

set -ex

DIR=$(pwd)

# works on Linux and Mac; see https://unix.stackexchange.com/questions/30091/fix-or-alternative-for-mktemp-in-os-x
WORK_DIR=`mktemp -d 2>/dev/null || mktemp -d -t 'tmpdir'`

if [[ ! "$WORK_DIR" || ! -d "$WORK_DIR" ]]; then
  echo "Could not create temp dir"
  exit 1
fi

function cleanup {      
  rm -rf "$WORK_DIR"
}

trap cleanup EXIT

get_abs_filename() {
  # $1 : relative filename
  echo "$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
}

JARABSPATH=$(get_abs_filename $1)

(cd $WORK_DIR && jar xf $JARABSPATH && jar cf tinyjavaxtools.jar 'javax/tools/StandardJavaFileManager$PathFactory.class' 'javax/tools/JavaFileManager.class' 'javax/tools/StandardJavaFileManager.class' 'javax/tools/FileManagerUtils.class' 'javax/tools/StandardLocation$1.class' 'javax/tools/FileManagerUtils$1.class' 'javax/tools/StandardLocation$2.class' 'javax/tools/JavaFileManager$Location.class' 'javax/tools/StandardLocation.class' 'javax/tools/FileManagerUtils$2.class' 'javax/lang/model/type/TypeKind$1.class' 'javax/lang/model/type/TypeKind.class' && cp tinyjavaxtools.jar $DIR)


