package com.github.okbuilds.core.util

import com.github.okbuilds.okbuck.OkBuckGradlePlugin
import org.gradle.api.logging.Logger

class CmdUtil {

    private CmdUtil() {}

    /**
     * Run a command
     */
    static String run(String command, boolean ignoreError = false,
                      Logger logger = OkBuckGradlePlugin.LOGGER) {
        logger.info(command)

        def stdoutBuffer = new StringBuffer(), stderrBuffer = new StringBuffer()

        Process process = command.execute()
        process.waitForProcessOutput(stdoutBuffer, stderrBuffer)

        String stdout = stdoutBuffer.toString()
        String stderr = stderrBuffer.toString()
        logger.info(stdout)

        int exitCode = process.exitValue()
        if (!ignoreError && exitCode != 0) {
            if (stderr != null && !stderr.trim().isEmpty()) {
                logger.error(stderr)
            }
            throw new Exception("${command} exited with ${exitCode}")
        }

        return stdout
    }
}
