// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk.logger;

public enum LogLevel {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    SILENT(5);

    private final int levelInt;

    LogLevel(int i) {
        this.levelInt = i;
    }

    public int toInt() {
        return levelInt;
    }
}
