package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal employee reference (id + name) for list/picker endpoints.
 *
 * <p>The full {@link KaryawanDto} carries PII (address, phone, email, birth/joining data). Endpoints
 * reachable by every staff member must return this instead, so a KARYAWAN cannot enumerate colleagues'
 * personal details just to fill a dropdown.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RegisterForReflection
public class KaryawanSummaryDto {

    private Long id;

    private String namaKaryawan;
}
