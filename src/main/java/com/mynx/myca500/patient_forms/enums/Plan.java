package com.mynx.myca500.patient_forms.enums;

public enum Plan {
    FREE(false),
    PLUS(true);

    private final boolean invitesAllowed;

    Plan(boolean invitesAllowed) {
        this.invitesAllowed = invitesAllowed;
    }

    public boolean allowsInvites() {
        return invitesAllowed;
    }
}
