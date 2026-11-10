package com.frozenheart.backend.modules.session.service;

import java.util.regex.Pattern;

public final class SessionCodeSearch {
    private static final Pattern FULL_CODE = Pattern.compile("\\S+_[a-zA-Z0-9]+_[0-9]+_([0-9]+)");

    private SessionCodeSearch() {
    }

    public static String normalize(String query) {
        String trimmed = query == null ? "" : query.strip();
        return trimmed.startsWith("#") ? trimmed.substring(1).stripLeading() : trimmed;
    }

    public static Long sessionId(String code) {
        var match = FULL_CODE.matcher(code);
        if (!match.matches()) return null;
        try {
            return Long.valueOf(match.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
