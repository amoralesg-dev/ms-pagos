package com.rassini.pagos.util;

import java.util.Arrays;
import java.util.List;

public final class BuUtils {

    private BuUtils() {
    }

    public static boolean isAll(String bu) {
        return bu != null &&
               "ALL".equalsIgnoreCase(bu.trim());
    }

    public static List<String> splitBus(String bu) {

        if (bu == null || bu.isBlank()) {
            return List.of();
        }

        return Arrays.stream(bu.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}