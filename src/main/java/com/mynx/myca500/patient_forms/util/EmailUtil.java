package com.mynx.myca500.patient_forms.util;

import java.util.Locale;

public final class EmailUtil {

    private EmailUtil() {
    }

    public static String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.charAt(0) + "****" + email.substring(at);
    }
}
