package com.uber.okbuck.config;

import java.io.PrintStream;

public abstract class BuckConfigFile {

    /**
     * Print this file's content into the printer.
     * @param printer The print stream to use.
     */
    public abstract void print(PrintStream printer);
}
