package com.uber.okbuck.example.javalib;

import org.apache.avro.TypeEnum;
import org.apache.avro.ipc.HandshakeMatch;

public class DummyJavaClass {
  public void dummyMethod() {
    // Added to make sure classifiers work properly
    // From avro-ipc jar
    System.out.println(HandshakeMatch.BOTH);

    // From avro-ipc tests jar
    System.out.println(TypeEnum.a);
  }

  private void dummyCall(DummyInterface dummyInterface, String val) {
    dummyInterface.call(val);
  }

  public interface DummyInterface {
    void call(String v);
  }

  private static class DummyObject {
    private String lang;
  }
}
