package com.uber.okbuck.config;

import java.io.PrintStream;

public abstract class BuckConfigFile {

    /**
     * Print this file's content into the printer.
     */
    public abstract void print(PrintStream printer);
}
