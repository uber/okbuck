package com.github.okbuilds.okbuck.example.common;

public class Calc {
    private final CalcMonitor mCalcMonitor;

    public Calc(CalcMonitor calcMonitor) {
        mCalcMonitor = calcMonitor;
    }

    public int add(int a, int b) {
        mCalcMonitor.addCalled(BuildConfig.COMMON_CONFIG);
        return a + b;
    }
}
