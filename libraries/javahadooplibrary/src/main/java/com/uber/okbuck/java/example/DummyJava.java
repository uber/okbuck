package com.uber.okbuck.java.example;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class DummyJava {

  private static final Logger logger = Logger.getLogger(DummyJava.class.getName());

  static int add(int a, int b) {
    logger.setLevel(Level.WARN);
    return a + b;
  }
}
