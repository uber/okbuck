package com.uber.okbuck.example.common;

public class Calc {
  private final CalcMonitor mCalcMonitor;

  public Calc(CalcMonitor calcMonitor) {
    mCalcMonitor = calcMonitor;
  }

  public int add(int a, int b) {
    mCalcMonitor.addCalled("flavor");
    return a + b;
  }
}
