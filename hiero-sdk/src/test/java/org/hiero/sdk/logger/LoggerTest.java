// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.logger;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoggerTest {

    private Logger logger;

    private org.slf4j.Logger internalLogger;

    @BeforeEach
    void setup() {
        internalLogger = mock(org.slf4j.Logger.class);
        logger = new Logger(LogLevel.TRACE);
        logger.setLogger(internalLogger);
    }

    @Test
    void logsTrace() {
        logger.trace("log");
        verify(internalLogger, times(1)).trace(any(), any(Object[].class));
    }

    @Test
    void doesNotLogTraceIfNotEnabled() {
        logger.setLevel(LogLevel.ERROR);
        logger.trace("log");
        verify(internalLogger, times(0)).trace(any(), any(Object[].class));
    }

    @Test
    void logsDebug() {
        logger.debug("log");
        verify(internalLogger, times(1)).debug(any(), any(Object[].class));
    }

    @Test
    void doesNotLogDebugIfNotEnabled() {
        logger.setLevel(LogLevel.ERROR);
        logger.debug("log");
        verify(internalLogger, times(0)).debug(any(), any(Object[].class));
    }

    @Test
    void logsInfo() {
        logger.info("log");
        verify(internalLogger, times(1)).info(any(), any(Object[].class));
    }

    @Test
    void doesNotLogInfoIfNotEnabled() {
        logger.setLevel(LogLevel.ERROR);
        logger.info("log");
        verify(internalLogger, times(0)).info(any(), any(Object[].class));
    }

    @Test
    void logsWarn() {
        logger.warn("log");
        verify(internalLogger, times(1)).warn(any(), any(Object[].class));
    }

    @Test
    void doesNotLogWarnIfNotEnabled() {
        logger.setLevel(LogLevel.ERROR);
        logger.warn("log");
        verify(internalLogger, times(0)).warn(any(), any(Object[].class));
    }

    @Test
    void logsError() {
        logger.error("log");
        verify(internalLogger, times(1)).error(any(), any(Object[].class));
    }

    @Test
    void doesNotLogErrorIfSilent() {
        logger.setSilent(true);
        logger.error("log");
        verify(internalLogger, times(0)).error(any(), any(Object[].class));
    }

    @Test
    void logsWhenUnsilenced() {
        logger.setSilent(true);
        logger.error("log");
        logger.setSilent(false);
        logger.warn("log");
        verify(internalLogger, times(0)).error(any(), any(Object[].class));
        verify(internalLogger, times(1)).warn(any(), any(Object[].class));
    }
}
