package com.uber.okbuck.core.util;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void copyResourceToProject(String resource, File destination, Map<String, String> templates) {
        try {
            InputStream inputStream = FileUtil.class.getResourceAsStream(resource);
            destination.getParentFile().mkdirs();
            InputStreamReader reader = new InputStreamReader(inputStream);
            TemplateReader replacingReader = new TemplateReader(reader, new TemplateMapResolver(templates));

            OutputStream outputStream = new FileOutputStream(destination);
            IOUtils.copy(replacingReader, outputStream, Charset.defaultCharset());
            IOUtils.closeQuietly(inputStream);
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

        final PushbackReader pushbackReader;
        final TemplateResolver templateResolver;
        final StringBuilder templateBuffer = new StringBuilder();

        String template = null;
        int templateIndex = 0;

        TemplateReader(Reader source, TemplateResolver resolver) {
            this.pushbackReader = new PushbackReader(source, 2);
            this.templateResolver = resolver;
        }

        @Override
        public int read(@NotNull CharBuffer target) throws IOException {
            throw new RuntimeException("Operation Not Supported");
        }

        @Override
        public int read() throws IOException {
            if (template != null) {
                if (templateIndex < template.length()) {
                    return template.charAt(this.templateIndex++);
                }
                if (templateIndex == template.length()) {
                    template = null;
                    templateIndex = 0;
                }
            }

            int data = pushbackReader.read();
            if (data != '$') { return data; }

            data = pushbackReader.read();
            if (data != '{') {
                pushbackReader.unread(data);
                return '$';
            }
            templateBuffer.delete(0, templateBuffer.length());

            data = pushbackReader.read();
            while (data != '}') {
                templateBuffer.append((char) data);
                data = pushbackReader.read();
            }

            template = templateResolver
                    .resolveTemplate(templateBuffer.toString());

            if (template == null) {
                template = "${" + templateBuffer.toString() + "}";
            }
            if (template.length() == 0) {
                return read();
            }
            return template.charAt(templateIndex++);
        }

        @Override
        public int read(@NotNull char cbuf[]) throws IOException {
            return read(cbuf, 0, cbuf.length);
        }

        @Override
        public int read(@NotNull char cbuf[], int off, int len) throws IOException {
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
            pushbackReader.close();
        }

        @Override
        public long skip(long n) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean ready() throws IOException {
            return pushbackReader.ready();
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
