package com.uber.okbuck.scala.example

import org.junit.runner.RunWith

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DummyScalaTest extends AnyFlatSpec {
  "Hello" should "say hello" in {
      assert(DummyScala.hello == "hello")
   }
}
