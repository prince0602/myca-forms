package com.mynx.myca500.patient_forms.enums;

public enum Plan {
    FREE(false, 1),
    PLUS(true, 7);

    private final boolean invitesAllowed;
    private final int maxUsers;

    Plan(boolean invitesAllowed, int maxUsers) {
        this.invitesAllowed = invitesAllowed;
        this.maxUsers = maxUsers;
    }

    public boolean allowsInvites() {
        return invitesAllowed;
    }

    public int maxUsers() {
        return maxUsers;
    }
}
