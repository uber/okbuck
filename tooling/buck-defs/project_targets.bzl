def project_android_binary(
    aapt_mode='aapt2',
    **kwargs
    ):
  native.android_binary(
    aapt_mode=aapt_mode,
    **kwargs
  )
