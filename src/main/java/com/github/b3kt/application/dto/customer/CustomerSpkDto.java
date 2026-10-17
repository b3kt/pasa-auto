package com.github.b3kt.application.dto.customer;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

/**
 * Reduced, customer-safe view of a work order: status and identifying info only. Deliberately
 * hand-written (not MapStruct-subtracted from the staff {@code SpkDto}) so pricing/parts can't
 * accidentally leak through a shared mapper later.
 */
@Getter
@Setter
@RegisterForReflection
public class CustomerSpkDto {
    private Long id;
    private String noSpk;
    private String tanggalJamSpk;
    private String nopol;
    private String statusSpk;
    private String keterangan;
}
