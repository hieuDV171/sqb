package com.frozenheart.backend.core.security;

public enum AccountState {
    EMAIL_VERIFICATION_REQUIRED,
    PASSWORD_CHANGE_REQUIRED,
    ACTIVE;

    public static AccountState from(boolean verified, boolean profileCompleted) {
        if (!verified) {
            return EMAIL_VERIFICATION_REQUIRED;
        }
        if (!profileCompleted) {
            return PASSWORD_CHANGE_REQUIRED;
        }
        return ACTIVE;
    }
}
