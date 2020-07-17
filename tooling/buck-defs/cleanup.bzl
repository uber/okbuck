JARS_TO_CLEANUP = [
  '.okbuck/ext/org/hamcrest:hamcrest-core.jar',
  '.okbuck/ext/org/hamcrest:hamcrest-integration.jar',
  '.okbuck/ext/com/google/code/gson:gson.jar'
]


ENTRIES_TO_DELETE = " ".join([
  'LICENSE.txt',
  'LICENSE',
  'NOTICE',
  'asm-license.txt',
  'module-info.class',
])


def cleaned_up_jar(
    name,
    binary_jar):
  full_base_path = native.package_name() + ':' + name
  if full_base_path in JARS_TO_CLEANUP:
    native.genrule(
        name = name + "_cleanup",
        srcs = [binary_jar],
        out = name,
        cmd = "cp $SRCS $OUT; zip -dq $OUT {} || true".format(ENTRIES_TO_DELETE),
    )
    return ":" + name + "_cleanup"
  else:
    return binary_jar
