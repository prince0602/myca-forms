package com.mynx.myca500.patient_forms.util;

import java.security.SecureRandom;

public final class PublicIdUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final char[] WORKSPACE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();

    private PublicIdUtil() {
    }

    public static String newAccountId() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 2; i++) {
            sb.append(LETTERS[RANDOM.nextInt(LETTERS.length)]);
        }
        for (int i = 0; i < 10; i++) {
            sb.append((char) ('0' + RANDOM.nextInt(10)));
        }
        return sb.toString();
    }

    public static String newWorkspaceId() {
        char[] id = new char[6];
        for (int i = 0; i < id.length; i++) {
            id[i] = WORKSPACE_ALPHABET[RANDOM.nextInt(WORKSPACE_ALPHABET.length)];
        }
        return new String(id);
    }

    public static String formatAccountId(String raw) {
        if (raw == null || raw.length() != 12) {
            return raw;
        }
        return raw.substring(0, 2) + " " + raw.substring(2, 6) + " " + raw.substring(6, 10) + " " + raw.substring(10);
    }

    public static String normalizeAccountId(String input) {
        return input == null ? "" : input.replace(" ", "").toUpperCase();
    }
}
