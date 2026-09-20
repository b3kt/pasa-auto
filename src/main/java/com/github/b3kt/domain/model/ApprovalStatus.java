package com.github.b3kt.domain.model;

/**
 * Whether an account may be used yet.
 *
 * <p>Accounts an admin creates are {@link #APPROVED} from the start; only the ones the Google
 * callback creates for an unknown email begin as {@link #PENDING}, waiting for an Owner. The
 * distinction from the {@code active} flag is deliberate: {@code active=false} means an Owner
 * switched an account off, {@code PENDING} means they have not looked at it yet.
 */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
