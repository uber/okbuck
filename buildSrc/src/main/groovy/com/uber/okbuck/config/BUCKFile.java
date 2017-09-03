package com.uber.okbuck.config;

import com.fizzed.rocker.runtime.OutputStreamOutput;
import com.uber.okbuck.template.core.Rule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public final class BUCKFile {

    private static final byte[] NEWLINE = System.lineSeparator().getBytes();

    private final List<Rule> rules;
    private final File buckFile;

    public BUCKFile(List<Rule> rules, File buckFile) {
        this.rules = rules;
        this.buckFile = buckFile;
    }

    public void print() throws IOException {
        final OutputStream os = new FileOutputStream(buckFile);

        for (Rule rule : rules) {
            rule.render(os);
            os.write(NEWLINE);
        }
        os.flush();
    }
}
