package com.uber.okbuck.example

class InternalTest {

  // This should compile because tests should be allowed to access `internal` elements of their main
  // source dir
  val internalAccess = MainActivity.TEST_INTERNAL

}
