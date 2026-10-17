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
 *
 * <p>{@code status} is {@link #STATUS_REGISTERED}, or {@link #STATUS_PENDING_APPROVAL} when linking the
 * existing record needs staff approval first (its contact details conflict with the account's); the
 * vehicle is then not registered yet.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@RegisterForReflection
public class VehicleRegistrationResultDto {

    public static final String STATUS_REGISTERED = "REGISTERED";
    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";

    private String status;
    private List<KendaraanOwnershipDto> vehicles;
    private boolean relinked;

    public static VehicleRegistrationResultDto registered(List<KendaraanOwnershipDto> vehicles, boolean relinked) {
        return new VehicleRegistrationResultDto(STATUS_REGISTERED, vehicles, relinked);
    }

    public static VehicleRegistrationResultDto pendingApproval(List<KendaraanOwnershipDto> vehicles) {
        return new VehicleRegistrationResultDto(STATUS_PENDING_APPROVAL, vehicles, false);
    }
}
