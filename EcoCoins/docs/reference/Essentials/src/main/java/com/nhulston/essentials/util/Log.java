package com.nhulston.essentials.util;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public final class Log {
    private static HytaleLogger logger;

    // ANSI color codes
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    private Log() {}

    public static void init(@Nonnull HytaleLogger logger) {
        Log.logger = logger;
    }

    public static void info(@Nonnull String message) {
        logger.at(Level.INFO).log(GREEN + message + RESET);
    }

    public static void warning(@Nonnull String message) {
        logger.at(Level.WARNING).log(YELLOW + message + RESET);
    }

    public static void error(@Nonnull String message) {
        logger.at(Level.SEVERE).log(RED + message + RESET);
    }

    public static void error(@Nonnull String message, @Nonnull Throwable throwable) {
        logger.at(Level.SEVERE).withCause(throwable).log(RED + message + RESET);
    }
}
