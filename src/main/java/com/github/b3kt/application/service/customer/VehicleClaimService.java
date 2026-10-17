package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.VehicleClaimAdminDto;
import com.github.b3kt.application.dto.customer.VehicleClaimResultDto;
import com.github.b3kt.application.dto.pazaauto.KendaraanOwnershipDto;
import com.github.b3kt.application.helper.NopolNormalizer;
import com.github.b3kt.application.service.pazaauto.KendaraanOwnershipService;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerVehicleClaimRequestEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerVehicleClaimRequestRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Handles a customer's claim on a plate already registered to another customer. Depending on
 * {@code app.customer.vehicle-claim.mode} the claim either transfers ownership immediately or is
 * queued for Admin/Owner review; on approval the ownership is transferred to the claimant.
 */
@ApplicationScoped
public class VehicleClaimService {

    static final String MODE_IMMEDIATE = "IMMEDIATE";

    @Inject
    CustomerAccountRepository accountRepository;

    @Inject
    CustomerVehicleClaimRequestRepository claimRequestRepository;

    @Inject
    TbPelangganRepository pelangganRepository;

    @Inject
    KendaraanOwnershipService kendaraanOwnershipService;

    @ConfigProperty(name = "app.customer.vehicle-claim.mode", defaultValue = "APPROVAL")
    String claimMode;

    @Transactional
    public VehicleClaimResultDto requestClaim(Long customerAccountId, String nopol) {
        CustomerAccountEntity account = accountRepository.findByIdOptional(customerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        Long pelangganId = account.getPelangganId();
        if (pelangganId == null) {
            throw new IllegalArgumentException("Link your account to a customer record first");
        }

        String norm = NopolNormalizer.normalize(nopol);
        if (norm.isEmpty()) {
            throw new IllegalArgumentException("nopol is required");
        }

        KendaraanOwnershipDto owner = kendaraanOwnershipService.getVehicleByNopol(norm);
        if (owner == null || owner.getIdPelanggan() == null) {
            throw new IllegalArgumentException("Vehicle " + norm + " is not registered to any customer");
        }
        if (Objects.equals(owner.getIdPelanggan(), pelangganId)) {
            throw new IllegalArgumentException("Vehicle " + norm + " is already registered to your account");
        }

        if (isImmediate()) {
            kendaraanOwnershipService.transfer(norm, pelangganId, LocalDate.now(), "Vehicle claim (immediate)");
            return new VehicleClaimResultDto(VehicleClaimResultDto.STATUS_LINKED,
                    "Vehicle " + norm + " has been linked to your account");
        }

        if (claimRequestRepository.findPendingByCustomerAccount(customerAccountId).isPresent()) {
            throw new IllegalArgumentException("You already have a pending vehicle claim request");
        }
        if (claimRequestRepository.findPendingByNopol(norm).isPresent()) {
            throw new IllegalArgumentException("A claim for vehicle " + norm + " is already awaiting approval");
        }

        CustomerVehicleClaimRequestEntity request = new CustomerVehicleClaimRequestEntity();
        request.setCustomerAccountId(customerAccountId);
        request.setPelangganId(pelangganId);
        request.setNopol(norm);
        request.setCurrentOwnerPelangganId(owner.getIdPelanggan());
        claimRequestRepository.persist(request);

        return new VehicleClaimResultDto(VehicleClaimResultDto.STATUS_PENDING_APPROVAL,
                "Your claim for vehicle " + norm + " has been sent for staff approval");
    }

    public List<VehicleClaimAdminDto> listRequests(String status) {
        return claimRequestRepository.findByStatus(status).stream().map(this::toAdminDto).toList();
    }

    @Transactional
    public void approve(Long requestId, String reviewedBy) {
        CustomerVehicleClaimRequestEntity request = pendingRequestOrThrow(requestId);

        KendaraanOwnershipDto owner = kendaraanOwnershipService.getVehicleByNopol(request.getNopol());
        if (owner == null || owner.getIdPelanggan() == null) {
            throw new IllegalArgumentException("Vehicle " + request.getNopol()
                    + " is no longer registered to another customer; the customer can register it directly");
        }
        if (!Objects.equals(owner.getIdPelanggan(), request.getPelangganId())) {
            kendaraanOwnershipService.transfer(request.getNopol(), request.getPelangganId(),
                    LocalDate.now(), "Vehicle claim approved");
        }

        request.setStatus(CustomerVehicleClaimRequestEntity.STATUS_APPROVED);
        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(LocalDateTime.now());
    }

    @Transactional
    public void reject(Long requestId, String reviewedBy, String note) {
        CustomerVehicleClaimRequestEntity request = pendingRequestOrThrow(requestId);
        request.setStatus(CustomerVehicleClaimRequestEntity.STATUS_REJECTED);
        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNote(note);
    }

    private boolean isImmediate() {
        return MODE_IMMEDIATE.equalsIgnoreCase(claimMode);
    }

    private CustomerVehicleClaimRequestEntity pendingRequestOrThrow(Long requestId) {
        CustomerVehicleClaimRequestEntity request = claimRequestRepository.findByIdOptional(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle claim request not found"));
        if (!CustomerVehicleClaimRequestEntity.STATUS_PENDING.equals(request.getStatus())) {
            throw new IllegalArgumentException("This request has already been reviewed");
        }
        return request;
    }

    private VehicleClaimAdminDto toAdminDto(CustomerVehicleClaimRequestEntity r) {
        VehicleClaimAdminDto dto = new VehicleClaimAdminDto();
        dto.setId(r.getId());
        dto.setCustomerAccountId(r.getCustomerAccountId());
        dto.setPelangganId(r.getPelangganId());
        dto.setNopol(r.getNopol());
        dto.setCurrentOwnerPelangganId(r.getCurrentOwnerPelangganId());
        dto.setStatus(r.getStatus());
        dto.setReviewedBy(r.getReviewedBy());
        dto.setReviewedAt(r.getReviewedAt());
        dto.setReviewNote(r.getReviewNote());
        dto.setCreatedAt(r.getCreatedAt());
        accountRepository.findByIdOptional(r.getCustomerAccountId()).ifPresent(a -> {
            dto.setCustomerEmail(a.getEmail());
            dto.setCustomerFullName(a.getFullName());
        });
        pelangganRepository.findByIdCached(r.getPelangganId())
                .ifPresent(p -> dto.setNamaPelanggan(p.getNamaPelanggan()));
        if (r.getCurrentOwnerPelangganId() != null) {
            pelangganRepository.findByIdCached(r.getCurrentOwnerPelangganId())
                    .ifPresent(p -> dto.setCurrentOwnerName(p.getNamaPelanggan()));
        }
        return dto;
    }
}
