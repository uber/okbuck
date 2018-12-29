load('//tooling/buck-defs:cleanup.bzl', 'cleaned_up_jar')
load('//.okbuck/defs:okbuck_targets.bzl', 'okbuck_prebuilt_jar')


def project_android_binary(
    aapt_mode='aapt2',
    **kwargs
    ):
  native.android_binary(
    aapt_mode=aapt_mode,
    **kwargs
  )


def project_prebuilt_jar(
    name,
    binary_jar,
    **kwargs
    ):
  binary_jar = cleaned_up_jar(name, binary_jar)
  okbuck_prebuilt_jar(
    name=name,
    binary_jar=binary_jar,
    **kwargs
  )
