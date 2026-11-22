package com.mynx.myca500.patient_forms.util;

public final class NameUtil {

    private NameUtil() {
    }

    public static String fullName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        return (first + " " + last).trim();
    }
}
