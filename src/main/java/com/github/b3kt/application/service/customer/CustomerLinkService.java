package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.LinkAccountRequest;
import com.github.b3kt.application.dto.customer.LinkRequestAdminDto;
import com.github.b3kt.application.dto.customer.LinkRequestResultDto;
import com.github.b3kt.application.dto.customer.LinkSearchResultDto;
import com.github.b3kt.application.dto.pazaauto.KendaraanOwnershipDto;
import com.github.b3kt.application.service.pazaauto.KendaraanOwnershipService;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerPelangganLinkRequestEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerPelangganLinkRequestRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Links a {@code CustomerAccountEntity} to its matching {@code tb_pelanggan} record: auto-link
 * when the target isn't already claimed, or queue for staff approval when it is. Strict 1:1 is
 * enforced by a DB partial unique index (see V22); this service only decides which branch to take.
 */
@ApplicationScoped
public class CustomerLinkService {

    @Inject
    CustomerAccountRepository accountRepository;

    @Inject
    CustomerPelangganLinkRequestRepository linkRequestRepository;

    @Inject
    TbPelangganRepository pelangganRepository;

    @Inject
    KendaraanOwnershipService kendaraanOwnershipService;

    public LinkSearchResultDto searchByNopol(String nopol) {
        KendaraanOwnershipDto owner = kendaraanOwnershipService.getVehicleByNopol(nopol);
        if (owner == null || owner.getIdPelanggan() == null) {
            return null;
        }
        // Only allow linking to existing pelanggan records that are not yet claimed by another customer account
        boolean alreadyClaimed = accountRepository.findByPelangganId(owner.getIdPelanggan()).isPresent();
        if (alreadyClaimed) {
            return null;
        }
        LinkSearchResultDto dto = new LinkSearchResultDto();
        dto.setPelangganId(owner.getIdPelanggan());
        dto.setNopol(owner.getNopol());
        dto.setMaskedName(maskName(owner.getNamaPelanggan()));
        dto.setMaskedPhone(maskContact(owner.getNoHpPelanggan()));
        return dto;
    }

    public List<LinkSearchResultDto> searchByContact(String phone, String email) {
        return pelangganRepository.findByContact(phone, email).stream()
                .filter(p -> accountRepository.findByPelangganId(p.getId()).isEmpty())
                .map(p -> {
                    LinkSearchResultDto dto = new LinkSearchResultDto();
                    dto.setPelangganId(p.getId());
                    dto.setNopol(p.getNopol());
                    dto.setMaskedName(maskName(p.getNamaPelanggan()));
                    dto.setMaskedPhone(maskContact(p.getNoHp() != null ? p.getNoHp() : p.getNoTelepon()));
                    dto.setMaskedEmail(maskContact(p.getEmail()));
                    return dto;
                })
                .toList();
    }

    @Transactional
    public LinkRequestResultDto requestLink(Long customerAccountId, LinkAccountRequest request) {
        CustomerAccountEntity account = accountRepository.findByIdOptional(customerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (account.isLinked()) {
            throw new IllegalArgumentException("Account is already linked to a customer record");
        }
        TbPelangganEntity pelanggan = pelangganRepository.findByIdOptional(request.getPelangganId())
                .orElseThrow(() -> new IllegalArgumentException("Pelanggan not found"));

        boolean alreadyClaimed = accountRepository.findByPelangganId(pelanggan.getId()).isPresent();
        if (!alreadyClaimed) {
            account.setPelangganId(pelanggan.getId());
            account.setLinkedAt(LocalDateTime.now());
            return new LinkRequestResultDto(LinkRequestResultDto.STATUS_LINKED,
                    "Your account is now linked to " + pelanggan.getNamaPelanggan());
        }

        if (linkRequestRepository.findPendingByCustomerAccount(customerAccountId).isPresent()) {
            throw new IllegalArgumentException("You already have a pending link request");
        }
        CustomerPelangganLinkRequestEntity linkRequest = new CustomerPelangganLinkRequestEntity();
        linkRequest.setCustomerAccountId(customerAccountId);
        linkRequest.setPelangganId(pelanggan.getId());
        linkRequest.setMatchType(request.getMatchType());
        linkRequest.setMatchValue(request.getMatchValue());
        linkRequestRepository.persist(linkRequest);
        return new LinkRequestResultDto(LinkRequestResultDto.STATUS_PENDING_APPROVAL,
                "This record is already linked to another account. Your request has been sent for staff approval.");
    }

    public List<LinkRequestAdminDto> listRequests(String status) {
        return linkRequestRepository.findByStatus(status).stream().map(this::toAdminDto).toList();
    }

    @Transactional
    public void approve(Long requestId, String reviewedBy) {
        CustomerPelangganLinkRequestEntity request = pendingRequestOrThrow(requestId);

        // Staff visually confirmed identity, so approving here forcibly releases the previous holder's link.
        accountRepository.findByPelangganId(request.getPelangganId())
                .ifPresent(previousHolder -> {
                    previousHolder.setPelangganId(null);
                    previousHolder.setLinkedAt(null);
                });

        CustomerAccountEntity account = accountRepository.findByIdOptional(request.getCustomerAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.setPelangganId(request.getPelangganId());
        account.setLinkedAt(LocalDateTime.now());

        request.setStatus(CustomerPelangganLinkRequestEntity.STATUS_APPROVED);
        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(LocalDateTime.now());
    }

    @Transactional
    public void reject(Long requestId, String reviewedBy, String note) {
        CustomerPelangganLinkRequestEntity request = pendingRequestOrThrow(requestId);
        request.setStatus(CustomerPelangganLinkRequestEntity.STATUS_REJECTED);
        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNote(note);
    }

    private CustomerPelangganLinkRequestEntity pendingRequestOrThrow(Long requestId) {
        CustomerPelangganLinkRequestEntity request = linkRequestRepository.findByIdOptional(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Link request not found"));
        if (!CustomerPelangganLinkRequestEntity.STATUS_PENDING.equals(request.getStatus())) {
            throw new IllegalArgumentException("This request has already been reviewed");
        }
        return request;
    }

    private LinkRequestAdminDto toAdminDto(CustomerPelangganLinkRequestEntity r) {
        LinkRequestAdminDto dto = new LinkRequestAdminDto();
        dto.setId(r.getId());
        dto.setCustomerAccountId(r.getCustomerAccountId());
        dto.setPelangganId(r.getPelangganId());
        dto.setMatchType(r.getMatchType());
        dto.setMatchValue(r.getMatchValue());
        dto.setStatus(r.getStatus());
        dto.setReviewedBy(r.getReviewedBy());
        dto.setReviewedAt(r.getReviewedAt());
        dto.setReviewNote(r.getReviewNote());
        dto.setCreatedAt(r.getCreatedAt());
        accountRepository.findByIdOptional(r.getCustomerAccountId()).ifPresent(a -> {
            dto.setCustomerEmail(a.getEmail());
            dto.setCustomerFullName(a.getFullName());
        });
        pelangganRepository.findByIdCached(r.getPelangganId()).ifPresent(p -> dto.setNamaPelanggan(p.getNamaPelanggan()));
        return dto;
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.length() <= 2) {
            return trimmed.charAt(0) + "*";
        }
        return trimmed.charAt(0) + "*".repeat(trimmed.length() - 2) + trimmed.charAt(trimmed.length() - 1);
    }

    @Transactional
    public void skipLinking(Long customerAccountId) {
        CustomerAccountEntity account = accountRepository.findByIdOptional(customerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (account.isLinked()) {
            throw new IllegalArgumentException("Account is already linked to a customer record");
        }
        if (account.getPelangganId() != null) {
            return;
        }
        // Start the customer's own record from the account profile; vehicles are added later.
        TbPelangganEntity pelanggan = PortalPelanggan.create(account);
        pelangganRepository.persist(pelanggan);
        account.setPelangganId(pelanggan.getId());
        account.setLinkedAt(LocalDateTime.now());
    }

    private static String maskContact(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        int visible = Math.min(3, trimmed.length());
        return "*".repeat(Math.max(0, trimmed.length() - visible)) + trimmed.substring(trimmed.length() - visible);
    }
}
