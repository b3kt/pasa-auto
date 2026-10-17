package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.CustomerVehicleRegisterRequest;
import com.github.b3kt.application.dto.customer.NopolCheckDto;
import com.github.b3kt.application.dto.customer.VehicleRegistrationResultDto;
import com.github.b3kt.application.dto.pazaauto.KendaraanOwnershipDto;
import com.github.b3kt.application.helper.Masking;
import com.github.b3kt.application.helper.NopolNormalizer;
import com.github.b3kt.application.helper.PhoneNormalizer;
import com.github.b3kt.application.service.pazaauto.KendaraanOwnershipService;
import com.github.b3kt.application.service.pazaauto.PelangganMergeService;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerPelangganLinkRequestEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerPelangganLinkRequestRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
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
 *   <li>plate owned by another customer's account (claimed), or the caller already has their own
 *       provisioned record -&gt; conflict, use the claim flow;</li>
 *   <li>plate on an unclaimed on-record customer, caller not yet provisioned -&gt; the portal shows the
 *       record ({@link NopolCheckDto#ON_RECORD}) and asks "is this yours?":
 *       <ul>
 *         <li>yes -&gt; link that record to the account, unless its phone/email on file contradict the
 *             account's, in which case the link goes to staff approval;</li>
 *         <li>no / no answer -&gt; only the vehicle is transferred to the caller's own record;</li>
 *       </ul></li>
 *   <li>otherwise -&gt; create/reuse the caller's own record from their account profile.</li>
 * </ul>
 *
 * A customer is provisioned once their linked {@code tb_pelanggan} is a staff record or owns a
 * vehicle. Until then it is a self-registered ({@code sumber = PORTAL}) record without vehicles, which
 * is either reused for the new plate or merged into the adopted on-record customer. Brand/type the
 * customer corrected in the form are applied to the vehicle in every outcome.
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

    @Inject
    CustomerPelangganLinkRequestRepository linkRequestRepository;

    private enum Outcome {
        OWNED_BY_SELF,
        OWNED_BY_OTHER,
        ADOPT,
        CREATE
    }

    private record Resolution(Outcome outcome, Long matchedPelangganId) {
    }

    /**
     * Read-only validation for the portal, aware of the adopt-vs-conflict distinction. For an adoptable
     * plate it returns the vehicle's brand/type and the masked name of the record it is on.
     */
    public NopolCheckDto checkNopol(Long customerAccountId, String nopol) {
        CustomerAccountEntity account = accountOrThrow(customerAccountId);
        String norm = NopolNormalizer.normalize(nopol);
        if (norm.isEmpty()) {
            throw new IllegalArgumentException("nopol is required");
        }
        return switch (resolve(account, norm).outcome()) {
            case OWNED_BY_SELF -> NopolCheckDto.ownedBySelf(norm);
            case OWNED_BY_OTHER -> NopolCheckDto.ownedByOther(norm);
            case ADOPT -> onRecord(norm);
            case CREATE -> NopolCheckDto.available(norm);
        };
    }

    private NopolCheckDto onRecord(String norm) {
        KendaraanOwnershipDto vehicle = ownershipService.getVehicleByNopol(norm);
        if (vehicle == null) {
            return NopolCheckDto.available(norm);
        }
        return NopolCheckDto.onRecord(norm, vehicle.getMerk(), blankToNull(vehicle.getJenis()),
                Masking.maskName(vehicle.getNamaPelanggan()));
    }

    @Transactional
    public VehicleRegistrationResultDto registerVehicle(Long customerAccountId, CustomerVehicleRegisterRequest request) {
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

        if (resolution.outcome() == Outcome.ADOPT) {
            TbPelangganEntity onRecord = pelangganRepository.findByIdOptional(resolution.matchedPelangganId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer record not found"));
            if (Boolean.TRUE.equals(request.getOwnRecord())) {
                return adoptRecord(account, myPelanggan, onRecord, norm, request);
            }
            return takeOverVehicle(account, myPelanggan, norm, request);
        }

        boolean relinked = false;
        TbPelangganEntity target = myPelanggan;
        if (target == null) {
            target = createOwnRecord(account);
            relinked = true;
        }
        fillNameFromAccount(target, account);
        pelangganRepository.getEntityManager().merge(target);
        pelangganRepository.invalidatePelangganCache();

        // The junction is authoritative; only open an ownership row when none exists yet.
        if (ownershipRepository.findCurrentByNopol(norm).isEmpty()) {
            ownershipService.attach(target.getId(), norm, blankToNull(request.getMerk()),
                    blankToNull(request.getJenis()), null, null);
        }
        return VehicleRegistrationResultDto.registered(
                ownershipService.listCurrentVehiclesByPelanggan(target.getId()), relinked);
    }

    /**
     * "Yes, this record is mine": link it to the account (folding the caller's own vehicle-less record
     * into it), unless the record's contact details contradict the account's - then staff decide.
     */
    private VehicleRegistrationResultDto adoptRecord(CustomerAccountEntity account, TbPelangganEntity myPelanggan,
                                                    TbPelangganEntity onRecord, String norm,
                                                    CustomerVehicleRegisterRequest request) {
        if (contactConflicts(onRecord, account)) {
            queueLinkRequest(account, onRecord, norm);
            List<KendaraanOwnershipDto> vehicles = myPelanggan == null ? List.of()
                    : ownershipService.listCurrentVehiclesByPelanggan(myPelanggan.getId());
            return VehicleRegistrationResultDto.pendingApproval(vehicles);
        }
        if (myPelanggan != null && !Objects.equals(myPelanggan.getId(), onRecord.getId())) {
            // Only reached while unprovisioned, so the caller's own record is a vehicle-less
            // self-registration: fold it (and the account link) into the adopted record.
            mergeService.merge(myPelanggan.getId(), onRecord.getId());
        }
        account.setPelangganId(onRecord.getId());
        account.setLinkedAt(LocalDateTime.now());
        accountRepository.getEntityManager().merge(account);
        pelangganRepository.invalidatePelangganCache();

        applyVehicleCorrections(onRecord.getId(), norm, request);
        return VehicleRegistrationResultDto.registered(
                ownershipService.listCurrentVehiclesByPelanggan(onRecord.getId()), true);
    }

    /**
     * "No, not my record" (or no answer): the record stays with its owner; only the vehicle's current
     * ownership moves to the caller's own record, e.g. a car bought second-hand.
     */
    private VehicleRegistrationResultDto takeOverVehicle(CustomerAccountEntity account, TbPelangganEntity myPelanggan,
                                                        String norm, CustomerVehicleRegisterRequest request) {
        boolean relinked = false;
        TbPelangganEntity target = myPelanggan;
        if (target == null) {
            target = createOwnRecord(account);
            relinked = true;
        }
        ownershipService.transfer(norm, target.getId(), LocalDate.now(),
                "Didaftarkan pelanggan melalui portal (bukan data pelanggan lama)");
        applyVehicleCorrections(target.getId(), norm, request);
        pelangganRepository.invalidatePelangganCache();
        return VehicleRegistrationResultDto.registered(
                ownershipService.listCurrentVehiclesByPelanggan(target.getId()), relinked);
    }

    /**
     * True when the record carries a phone or email and neither matches the account. A record with no
     * contact on file (most walk-ins) never conflicts.
     */
    private boolean contactConflicts(TbPelangganEntity record, CustomerAccountEntity account) {
        String recordPhone = PhoneNormalizer.normalize(record.getNoHp());
        String recordEmail = blankToNull(record.getEmail());
        if (recordPhone == null && recordEmail == null) {
            return false;
        }
        boolean phoneMatches = recordPhone != null
                && recordPhone.equals(PhoneNormalizer.normalize(account.getPhone()));
        boolean emailMatches = recordEmail != null && account.getEmail() != null
                && recordEmail.trim().equalsIgnoreCase(account.getEmail().trim());
        return !phoneMatches && !emailMatches;
    }

    private void queueLinkRequest(CustomerAccountEntity account, TbPelangganEntity onRecord, String norm) {
        if (linkRequestRepository.findPendingByCustomerAccount(account.getId()).isPresent()) {
            throw new IllegalArgumentException("You already have a pending link request");
        }
        CustomerPelangganLinkRequestEntity linkRequest = new CustomerPelangganLinkRequestEntity();
        linkRequest.setCustomerAccountId(account.getId());
        linkRequest.setPelangganId(onRecord.getId());
        linkRequest.setMatchType(CustomerPelangganLinkRequestEntity.MATCH_NOPOL);
        linkRequest.setMatchValue(norm);
        linkRequestRepository.persist(linkRequest);
    }

    /** Brand/type the customer picked (or corrected) in the form; no change when left empty. */
    private void applyVehicleCorrections(Long pelangganId, String norm, CustomerVehicleRegisterRequest request) {
        String merk = blankToNull(request.getMerk());
        if (merk != null) {
            ownershipService.updateVehicle(pelangganId, norm, merk, blankToNull(request.getJenis()));
        }
    }

    private TbPelangganEntity createOwnRecord(CustomerAccountEntity account) {
        TbPelangganEntity record = PortalPelanggan.create(account);
        pelangganRepository.persist(record);
        account.setPelangganId(record.getId());
        account.setLinkedAt(LocalDateTime.now());
        accountRepository.getEntityManager().merge(account);
        return record;
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

    /** Contacts are not copied: once linked, the account's phone/email are authoritative (ADR-0012). */
    private void fillNameFromAccount(TbPelangganEntity target, CustomerAccountEntity account) {
        if (isBlank(target.getNamaPelanggan()) && !isBlank(account.getFullName())) {
            target.setNamaPelanggan(account.getFullName());
        }
    }

    private CustomerAccountEntity accountOrThrow(Long customerAccountId) {
        return accountRepository.findByIdOptional(customerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
