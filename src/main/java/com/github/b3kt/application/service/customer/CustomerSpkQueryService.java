package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.CustomerSpkDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Read-only, pre-filtered view of a customer's own work orders. Every method takes the caller's
 * own {@code pelangganId} (resolved from the JWT by {@code CustomerPrincipal}, never a parameter
 * a client could substitute) and maps to {@link CustomerSpkDto}, which has no pricing/parts fields.
 */
@ApplicationScoped
public class CustomerSpkQueryService {

    @Inject
    TbSpkRepository spkRepository;

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
