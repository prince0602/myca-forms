package com.mynx.myca500.patient_forms.enums;

public enum Profession {
    ATTORNEY("Attorney"),
    PARALEGAL("Paralegal"),
    ACCOUNTANT("Accountant / CPA"),
    FINANCIAL_ADVISOR("Financial advisor"),
    INSURANCE_AGENT("Insurance agent"),
    REAL_ESTATE_AGENT("Real estate agent"),
    MORTGAGE_BROKER("Mortgage broker"),
    PHYSICIAN("Physician"),
    DENTIST("Dentist"),
    THERAPIST("Therapist / counselor"),
    CHIROPRACTOR("Chiropractor"),
    PHYSICAL_THERAPIST("Physical therapist"),
    NURSE("Nurse"),
    PHARMACIST("Pharmacist"),
    CONSULTANT("Consultant"),
    OTHER("Other");

    private final String label;

    Profession(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
