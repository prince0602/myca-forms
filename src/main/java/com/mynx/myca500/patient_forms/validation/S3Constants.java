package com.mynx.myca500.patient_forms.validation;

public final class S3Constants {
    public static final String FORMS_COMMON_FOLDER = "001";
    private S3Constants() {}

    /** Same rules S3Controller already enforces, now reusable. */
    public static boolean isSafeFileName(String f) {
        return f != null && !f.isBlank()
                && !f.contains("/") && !f.contains("\\") && !f.contains("..");
    }
}