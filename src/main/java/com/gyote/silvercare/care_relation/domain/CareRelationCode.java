package com.gyote.silvercare.care_relation.domain;

import java.util.Locale;

/** Invite-code formatting and normalization belong to the care-relation domain. */
public final class CareRelationCode {

    private CareRelationCode() {
    }

    public static String display(String raw) {
        if (raw == null || raw.length() != 6) {
            return raw == null ? "" : raw;
        }
        return raw.substring(0, 3) + "-" + raw.substring(3);
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
    }
}
