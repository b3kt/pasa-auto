package com.github.b3kt.application.dto.customer;

import lombok.Getter;
import lombok.Setter;

/**
 * Result of checking whether a plate number (nopol) is already registered to a customer.
 * Used by the customer portal to validate a plate before registering it.
 */
@Getter
@Setter
public class NopolCheckDto {

    public static final String AVAILABLE = "AVAILABLE";
    public static final String OWNED_BY_SELF = "OWNED_BY_SELF";
    public static final String OWNED_BY_OTHER = "OWNED_BY_OTHER";

    private String nopol;
    private String status;
    private boolean canRegister;

    public static NopolCheckDto available(String nopol) {
        NopolCheckDto dto = new NopolCheckDto();
        dto.setNopol(nopol);
        dto.setStatus(AVAILABLE);
        dto.setCanRegister(true);
        return dto;
    }

    public static NopolCheckDto ownedBySelf(String nopol) {
        NopolCheckDto dto = new NopolCheckDto();
        dto.setNopol(nopol);
        dto.setStatus(OWNED_BY_SELF);
        dto.setCanRegister(false);
        return dto;
    }

    public static NopolCheckDto ownedByOther(String nopol) {
        NopolCheckDto dto = new NopolCheckDto();
        dto.setNopol(nopol);
        dto.setStatus(OWNED_BY_OTHER);
        dto.setCanRegister(false);
        return dto;
    }
}
