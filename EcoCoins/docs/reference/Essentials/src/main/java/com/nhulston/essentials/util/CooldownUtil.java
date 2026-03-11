package com.nhulston.essentials.util;

import javax.annotation.Nonnull;

/**
 * Utility for formatting cooldown times.
 */
public final class CooldownUtil {

    private CooldownUtil() {}

    /**
     * Formats remaining cooldown as a human-readable string.
     *
     * @param seconds Remaining cooldown in seconds
     * @return Formatted string (e.g., "2h 30m 15s", "5m 30s", "45s")
     */
    @Nonnull
    public static String formatCooldown(long seconds) {
        if (seconds <= 0) {
            return "Ready";
        }

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
}
