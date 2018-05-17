package com.example.hellojni;

public class HelloJni {

  /* this is used to load the 'hello-jni' library on application
   * startup. The library has already been unpacked into
   * /data/data/com.example.hellojni/lib/libhello-jni.so at
   * installation time by the package manager.
   */
  static {
    System.loadLibrary("hello-jni");
  }

  /* A native method that is implemented by the
   * 'hello-jni' native library, which is packaged
   * with this application.
   */
  public static native String stringFromJNI();
}
