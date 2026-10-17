package com.github.b3kt.application.dto.customer;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

/**
 * Result of checking whether a plate number (nopol) is already registered to a customer.
 * Used by the customer portal to validate a plate before registering it.
 */
@Getter
@Setter
@RegisterForReflection
public class NopolCheckDto {

    public static final String AVAILABLE = "AVAILABLE";
    public static final String OWNED_BY_SELF = "OWNED_BY_SELF";
    public static final String OWNED_BY_OTHER = "OWNED_BY_OTHER";
    /**
     * The plate is on an existing customer record no portal account has claimed: the caller may say
     * whether that record is theirs. {@code merk}/{@code jenis} prefill the form; the owner name is masked.
     */
    public static final String ON_RECORD = "ON_RECORD";

    private String nopol;
    private String status;
    private boolean canRegister;
    private String merk;
    private String jenis;
    private String maskedOwnerName;

    public static NopolCheckDto available(String nopol) {
        NopolCheckDto dto = new NopolCheckDto();
        dto.setNopol(nopol);
        dto.setStatus(AVAILABLE);
        dto.setCanRegister(true);
        return dto;
    }

    public static NopolCheckDto onRecord(String nopol, String merk, String jenis, String maskedOwnerName) {
        NopolCheckDto dto = available(nopol);
        dto.setStatus(ON_RECORD);
        dto.setMerk(merk);
        dto.setJenis(jenis);
        dto.setMaskedOwnerName(maskedOwnerName);
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
