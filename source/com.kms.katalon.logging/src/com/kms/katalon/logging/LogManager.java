package com.kms.katalon.logging;

import java.io.UnsupportedEncodingException;

public class LogManager {
    private static final String UTF_8 = "utf-8";

    private static SystemLogger errorLogger;

    private static SystemLogger outputLogger;

    public static void active() {
        System.setErr(getErrorLogger());
        System.setOut(getOutputLogger());
    }

    public static SystemLogger getErrorLogger() {
        if (errorLogger == null) {
            try {
                errorLogger = new SystemLogger(System.err, LogMode.LOG, UTF_8);
            } catch (UnsupportedEncodingException e) {
                errorLogger = new SystemLogger(System.err, LogMode.LOG);
            }
        }
        return errorLogger;
    }

    public static SystemLogger getOutputLogger() {
        if (outputLogger == null) {
            try {
                outputLogger = new SystemLogger(System.out, LogMode.CONSOLE, UTF_8);
            } catch (UnsupportedEncodingException e) {
                outputLogger = new SystemLogger(System.out, LogMode.CONSOLE);
            }
        }
        return outputLogger;
    }

    public static void stop() {
        getErrorLogger().close();
        getOutputLogger().close();
    }
}
