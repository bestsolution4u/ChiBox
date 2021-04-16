package com.chibox.wellness.util;

public class TimeUtils {
    public static String formatDuration(int duration) {
        if (duration < 3600) return String.format("%02d:%02d", (duration % 3600) / 60, (duration % 60));
        return String.format("%d:%02d:%02d", duration / 3600, (duration % 3600) / 60, (duration % 60));
    }
}
