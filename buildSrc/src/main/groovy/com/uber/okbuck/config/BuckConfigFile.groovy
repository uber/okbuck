package com.uber.okbuck.config

abstract class BuckConfigFile {

    /**
     * Print this file's content into the printer.
     */
    abstract void print(PrintStream printer)
}
