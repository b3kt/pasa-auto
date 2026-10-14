package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.CustomerSpkDto;
import com.github.b3kt.application.dto.pazaauto.PenjualanPrintDto;
import com.github.b3kt.application.service.pazaauto.TbPenjualanService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Read-only, pre-filtered view of a customer's own work orders. Every method takes the caller's
 * own {@code pelangganId} (resolved from the JWT by {@code CustomerPrincipal}, never a parameter
 * a client could substitute).
 *
 * <p>The status views map to {@link CustomerSpkDto}, which deliberately has no pricing/parts
 * fields. The one exception is {@link #getInvoiceForPelanggan}: a customer is entitled to the
 * faktur penjualan of their own work order, so that method returns the full {@link PenjualanPrintDto}
 * (with amounts) after verifying the work order belongs to the caller.
 */
@ApplicationScoped
public class CustomerSpkQueryService {

    @Inject
    TbSpkRepository spkRepository;

    @Inject
    TbPenjualanService penjualanService;

    public List<CustomerSpkDto> listForPelanggan(Long pelangganId) {
        return spkRepository.find("pelangganId = ?1 order by id desc", pelangganId)
                .list().stream().map(this::toDto).toList();
    }

    public CustomerSpkDto getForPelanggan(Long id, Long pelangganId) {
        TbSpkEntity spk = spkRepository.findByIdOptional(id).orElse(null);
        if (spk == null || !pelangganId.equals(spk.getPelangganId())) {
            // Never distinguish "not found" from "not yours" - both must look identical to the caller.
            return null;
        }
        return toDto(spk);
    }

    /**
     * Returns the faktur penjualan for the caller's own work order, or {@code null} when the work
     * order doesn't exist, belongs to someone else, or has no penjualan raised against it yet.
     * A null return must be surfaced as a plain 404 - never distinguish "not yours" from "missing".
     */
    public PenjualanPrintDto getInvoiceForPelanggan(Long spkId, Long pelangganId) {
        TbSpkEntity spk = spkRepository.findByIdOptional(spkId).orElse(null);
        if (spk == null || !pelangganId.equals(spk.getPelangganId())) {
            return null;
        }
        TbPenjualanEntity penjualan = penjualanService.findByNoSpk(spk.getNoSpk());
        if (penjualan == null) {
            return null;
        }
        // Defence in depth: even though the penjualan was reached through the caller's own SPK,
        // never print an invoice whose owning pelanggan is someone else (stale/relinked data).
        if (penjualan.getPelangganId() != null && !pelangganId.equals(penjualan.getPelangganId())) {
            return null;
        }
        return penjualanService.buildPrintDto(penjualan.getNoPenjualan());
    }

    private CustomerSpkDto toDto(TbSpkEntity spk) {
        CustomerSpkDto dto = new CustomerSpkDto();
        dto.setId(spk.getId());
        dto.setNoSpk(spk.getNoSpk());
        dto.setTanggalJamSpk(spk.getTanggalJamSpk());
        dto.setNopol(spk.getNopol());
        dto.setStatusSpk(spk.getStatusSpk());
        dto.setKeterangan(spk.getKeterangan());
        return dto;
    }
}
