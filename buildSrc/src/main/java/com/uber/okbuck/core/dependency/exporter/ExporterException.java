package com.uber.okbuck.core.dependency.exporter;

public class ExporterException extends RuntimeException {
  public ExporterException(Exception e) {
    super(e);
  }
}
