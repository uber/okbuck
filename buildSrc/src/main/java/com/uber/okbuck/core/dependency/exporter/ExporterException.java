package com.uber.okbuck.core.dependency.exporter;

import java.io.IOException;

public class ExporterException extends RuntimeException {

  public ExporterException(IOException e) {
    super(e);
  }
}
