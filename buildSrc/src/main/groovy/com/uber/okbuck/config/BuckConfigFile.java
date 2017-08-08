package com.uber.okbuck.config;

import com.uber.okbuck.core.io.Printer;

abstract class BuckConfigFile {

    /**
     * Print this file's content into the printer.
     * @param printer The print stream to use.
     */
    public abstract void print(Printer printer);
}
