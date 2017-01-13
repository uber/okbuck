package com.uber.okbuck.core.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Map;

final class ReplaceUtil {

    static void copyResourceToProject(String resource, File destination, Map<String, String> templates) {
        try {
            InputStream inputStream = FileUtil.class.getResourceAsStream(resource);
            destination.getParentFile().mkdirs();
            InputStreamReader reader = new InputStreamReader(inputStream);
            TemplateReader replacingReader = new TemplateReader(reader, new TemplateMapResolver(templates));

            OutputStream outputStream = new FileOutputStream(destination);
            IOUtils.copy(replacingReader, outputStream, Charset.defaultCharset());
            IOUtils.closeQuietly(outputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private interface TemplateResolver {

        String resolveTemplate(String template);
    }

    private static final class TemplateMapResolver implements TemplateResolver {

        final Map<String, String> templateMap;

        private TemplateMapResolver(Map<String, String> templateMap) {
            this.templateMap = templateMap;
        }

        @Override
        public String resolveTemplate(String template) {
            return this.templateMap.get(template);
        }
    }

    private static final class TemplateReader extends Reader {

        PushbackReader pushbackReader = null;
        TemplateResolver templateResolver = null;
        StringBuilder templateBuffer = new StringBuilder();
        String template = null;
        int templateIndex = 0;

        TemplateReader(Reader source, TemplateResolver resolver) {
            this.pushbackReader = new PushbackReader(source, 2);
            this.templateResolver = resolver;
        }

        @Override
        public int read(CharBuffer target) throws IOException {
            throw new RuntimeException("Operation Not Supported");
        }

        @Override
        public int read() throws IOException {
            if (this.template != null) {
                if (this.templateIndex < this.template.length()) {
                    return this.template.charAt(this.templateIndex++);
                }
                if (this.templateIndex == this.template.length()) {
                    this.template = null;
                    this.templateIndex = 0;
                }
            }

            int data = this.pushbackReader.read();
            if (data != '$') { return data; }

            data = this.pushbackReader.read();
            if (data != '{') {
                this.pushbackReader.unread(data);
                return '$';
            }
            this.templateBuffer.delete(0, this.templateBuffer.length());

            data = this.pushbackReader.read();
            while (data != '}') {
                this.templateBuffer.append((char) data);
                data = this.pushbackReader.read();
            }

            this.template = this.templateResolver
                    .resolveTemplate(this.templateBuffer.toString());

            if (this.template == null) {
                this.template = "${" + this.templateBuffer.toString() + "}";
            }
            if (this.template.length() == 0) {
                return read();
            }
            return this.template.charAt(this.templateIndex++);


        }

        @Override
        public int read(char cbuf[]) throws IOException {
            return read(cbuf, 0, cbuf.length);
        }

        @Override
        public int read(char cbuf[], int off, int len) throws IOException {
            int charsRead = 0;
            for (int i = 0; i < len; i++) {
                int nextChar = read();
                if (nextChar == -1) {
                    if (charsRead == 0) {
                        charsRead = -1;
                    }
                    break;
                }
                charsRead = i + 1;
                cbuf[off + i] = (char) nextChar;
            }
            return charsRead;
        }

        @Override
        public void close() throws IOException {
            this.pushbackReader.close();
        }

        @Override
        public long skip(long n) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean ready() throws IOException {
            return this.pushbackReader.ready();
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public void mark(int readAheadLimit) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void reset() throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
