package com.uber.okbuck.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import okio.BufferedSink;
import okio.Okio;

public class FilePrinter implements Printer {

    private final BufferedSink sink;

    public FilePrinter(File file) throws FileNotFoundException {
        sink = Okio.buffer(Okio.sink(file));
    }

    @Override
    public void println(String content) {
        try {
            sink.writeUtf8(content);
            sink.writeUtf8(System.lineSeparator());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void println() {
        try {
            sink.writeUtf8(System.lineSeparator());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void flush() {
        try {
            sink.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
