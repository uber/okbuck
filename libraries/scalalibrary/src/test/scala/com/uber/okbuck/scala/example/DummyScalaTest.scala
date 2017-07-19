package com.uber.okbuck.scala.example

import org.junit.runner.RunWith

import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DummyScalaTest extends FlatSpec {
  "Hello" should "say hello" in {
      assert(DummyScala.hello == "hello")
   }
}
