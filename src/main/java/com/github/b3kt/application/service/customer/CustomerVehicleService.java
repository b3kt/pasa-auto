package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.NopolCheckDto;
import com.github.b3kt.application.dto.customer.VehicleRegistrationResultDto;
import com.github.b3kt.application.dto.pazaauto.KendaraanAttachRequest;
import com.github.b3kt.application.dto.pazaauto.KendaraanOwnershipDto;
import com.github.b3kt.application.helper.NopolNormalizer;
import com.github.b3kt.application.service.pazaauto.KendaraanOwnershipService;
import com.github.b3kt.application.service.pazaauto.PelangganMergeService;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Self-service vehicle registration. When a customer adds a plate, the plate is reconciled against
 * the {@code tb_pelanggan_kendaraan} ownership junction (the only source of vehicle data since
 * ADR-0011) so that an on-record customer is adopted instead of duplicated:
 *
 * <ul>
 *   <li>plate owned by the caller -&gt; already registered;</li>
 *   <li>plate owned by another customer's account (claimed) -&gt; conflict, use the claim flow;</li>
 *   <li>plate on an unclaimed on-record customer, caller not yet provisioned -&gt; adopt that record;</li>
 *   <li>otherwise -&gt; create/reuse the caller's own record from their account profile.</li>
 * </ul>
 *
 * A customer is provisioned once their linked {@code tb_pelanggan} is a staff record or owns a
 * vehicle. Until then it is a self-registered ({@code sumber = PORTAL}) record without vehicles, which
 * is either reused for the new plate or merged into the adopted on-record customer.
 */
@ApplicationScoped
public class CustomerVehicleService {

    @Inject
    CustomerAccountRepository accountRepository;

    @Inject
    TbPelangganRepository pelangganRepository;

    @Inject
    TbPelangganKendaraanRepository ownershipRepository;

    @Inject
    KendaraanOwnershipService ownershipService;

    @Inject
    PelangganMergeService mergeService;

    private enum Outcome {
        OWNED_BY_SELF,
        OWNED_BY_OTHER,
        ADOPT,
        CREATE
    }

    private record Resolution(Outcome outcome, Long matchedPelangganId) {
    }

    /** Read-only validation for the portal, aware of the adopt-vs-conflict distinction. */
    public NopolCheckDto checkNopol(Long customerAccountId, String nopol) {
        CustomerAccountEntity account = accountOrThrow(customerAccountId);
        String norm = NopolNormalizer.normalize(nopol);
        if (norm.isEmpty()) {
            throw new IllegalArgumentException("nopol is required");
        }
        return switch (resolve(account, norm).outcome()) {
            case OWNED_BY_SELF -> NopolCheckDto.ownedBySelf(norm);
            case OWNED_BY_OTHER -> NopolCheckDto.ownedByOther(norm);
            default -> NopolCheckDto.available(norm);
        };
    }

    @Transactional
    public VehicleRegistrationResultDto registerVehicle(Long customerAccountId, KendaraanAttachRequest request) {
        CustomerAccountEntity account = accountOrThrow(customerAccountId);
        String norm = NopolNormalizer.normalize(request.getNopol());
        if (norm.isEmpty()) {
            throw new IllegalArgumentException("nopol is required");
        }

        Resolution resolution = resolve(account, norm);
        if (resolution.outcome() == Outcome.OWNED_BY_SELF) {
            throw new IllegalArgumentException("Vehicle " + norm + " is already registered to your account");
        }
        if (resolution.outcome() == Outcome.OWNED_BY_OTHER) {
            throw new IllegalArgumentException("Vehicle " + norm
                    + " is registered to another customer - file a claim to take it over");
        }

        Long myPelangganId = account.getPelangganId();
        TbPelangganEntity myPelanggan = myPelangganId == null ? null
                : pelangganRepository.findByIdOptional(myPelangganId).orElse(null);

        boolean relinked = false;
        TbPelangganEntity target;
        if (resolution.outcome() == Outcome.ADOPT) {
            target = pelangganRepository.findByIdOptional(resolution.matchedPelangganId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer record not found"));
            if (!Objects.equals(myPelangganId, target.getId())) {
                if (myPelanggan != null) {
                    // ADOPT is only reached while unprovisioned, so the caller's own record is a
                    // vehicle-less self-registration: fold it (and the account link) into the target.
                    mergeService.merge(myPelanggan.getId(), target.getId());
                }
                account.setPelangganId(target.getId());
                account.setLinkedAt(LocalDateTime.now());
                accountRepository.getEntityManager().merge(account);
                relinked = true;
            }
        } else if (myPelanggan != null) {
            // Already provisioned, or a reusable self-registered record: keep the customer's own record.
            target = myPelanggan;
        } else {
            target = PortalPelanggan.create(account);
            pelangganRepository.persist(target);
            account.setPelangganId(target.getId());
            account.setLinkedAt(LocalDateTime.now());
            accountRepository.getEntityManager().merge(account);
            relinked = true;
        }

        // Vehicle data is not written here: tb_pelanggan.nopol/merk/jenis are derived from the
        // ownership row opened below (trg_pk_sync_legacy_vehicle).
        fillBlanksFromAccount(target, account);
        pelangganRepository.getEntityManager().merge(target);
        pelangganRepository.invalidatePelangganCache();

        // The junction is authoritative; only open an ownership row when none exists yet
        // (adopting a record that already carries this plate keeps its existing current row).
        if (ownershipRepository.findCurrentByNopol(norm).isEmpty()) {
            ownershipService.attach(target.getId(), norm, request.getMerk(), request.getJenis(),
                    request.getTanggalMulai(), request.getKeterangan());
        }

        List<KendaraanOwnershipDto> vehicles =
                ownershipService.listCurrentVehiclesByPelanggan(target.getId());
        return new VehicleRegistrationResultDto(vehicles, relinked);
    }

    private Resolution resolve(CustomerAccountEntity account, String norm) {
        Long myId = account.getPelangganId();
        boolean provisioned = isProvisioned(myId);

        Optional<TbPelangganKendaraanEntity> current = ownershipRepository.findCurrentByNopol(norm);
        if (current.isPresent()) {
            Long owner = current.get().getPelangganId();
            if (Objects.equals(owner, myId)) {
                return new Resolution(Outcome.OWNED_BY_SELF, owner);
            }
            if (provisioned || isClaimedByOtherAccount(owner)) {
                return new Resolution(Outcome.OWNED_BY_OTHER, owner);
            }
            return new Resolution(Outcome.ADOPT, owner);
        }

        return new Resolution(Outcome.CREATE, null);
    }

    /**
     * A staff record is always provisioned (it is real workshop data, never replaced by adoption); a
     * self-registered record becomes provisioned once it owns a vehicle.
     */
    private boolean isProvisioned(Long pelangganId) {
        if (pelangganId == null) {
            return false;
        }
        return pelangganRepository.findByIdOptional(pelangganId)
                .map(p -> !p.isPortalSourced()
                        || !ownershipRepository.findByPelangganIdCurrent(pelangganId).isEmpty())
                .orElse(false);
    }

    private boolean isClaimedByOtherAccount(Long pelangganId) {
        return accountRepository.findByPelangganId(pelangganId).isPresent();
    }

    private void fillBlanksFromAccount(TbPelangganEntity target, CustomerAccountEntity account) {
        if (isBlank(target.getNamaPelanggan()) && !isBlank(account.getFullName())) {
            target.setNamaPelanggan(account.getFullName());
        }
        if (isBlank(target.getNoHp()) && !isBlank(account.getPhone())) {
            target.setNoHp(account.getPhone());
        }
        if (isBlank(target.getEmail()) && !isBlank(account.getEmail())) {
            target.setEmail(account.getEmail());
        }
    }

    private CustomerAccountEntity accountOrThrow(Long customerAccountId) {
        return accountRepository.findByIdOptional(customerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
