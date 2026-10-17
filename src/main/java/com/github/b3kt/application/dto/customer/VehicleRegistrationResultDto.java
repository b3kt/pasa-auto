package com.github.b3kt.application.dto.customer;

import com.github.b3kt.application.dto.pazaauto.KendaraanOwnershipDto;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Outcome of registering a vehicle from the self-service portal.
 *
 * <p>{@code relinked} is set when provisioning moved the account onto a different (adopted)
 * {@code tb_pelanggan} record; the client must then refresh its access token because the
 * {@code pelangganId} claim embedded in it is now stale.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@RegisterForReflection
public class VehicleRegistrationResultDto {

    private List<KendaraanOwnershipDto> vehicles;
    private boolean relinked;
}
