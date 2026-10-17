package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.pazaauto.PelangganDto;
import com.github.b3kt.application.mapper.pazaauto.PelangganMapper;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.integration.IntegrationTestBase;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Staff edits of a customer go through {@link TbPelangganService#update}, which (unlike
 * {@code AbstractCrudService}) saves via the {@code Pelanggan} domain model. Neither the DTO nor the
 * domain model carries the row version, so the entity reaching {@code merge} has {@code version == null};
 * this used to fail every update with an optimistic-lock error (HTTP 400 "changed by someone else").
 */
@QuarkusTest
class TbPelangganServiceUpdateIntegrationTest extends IntegrationTestBase {

    @Inject
    TbPelangganService pelangganService;

    @Inject
    PelangganMapper mapper;

    /**
     * Reads the stored row. Outside a transaction a {@code @QuarkusTest} reuses one request-scoped
     * session for the whole test method, which would serve the first-loaded (stale) row from its cache.
     */
    private TbPelangganEntity reload(Long id) {
        return QuarkusTransaction.requiringNew().call(() -> pelangganService.findById(id));
    }

    private TbPelangganEntity createCustomer(String nopol) {
        PelangganDto dto = PelangganDto.builder()
                .namaPelanggan("QA Update " + nopol).nopol(nopol).merk("TOYOTA").jenis("AVANZA")
                .noHp("0812-0000-1111").build();
        return pelangganService.create(mapper.toEntity(dto));
    }

    @Test
    @DisplayName("update() with an entity mapped from the staff form's DTO succeeds")
    void updateFromDto() {
        TbPelangganEntity created = createCustomer("QU1001A");

        // What the staff page sends back: the DTO it received, edited.
        PelangganDto edited = mapper.toDto(reload(created.getId()));
        edited.setNamaPelanggan("QA Update Renamed");
        edited.setAlamat("Jl. Baru 1");
        TbPelangganEntity updated = pelangganService.update(created.getId(), mapper.toEntity(edited));

        assertNotNull(updated);
        TbPelangganEntity stored = reload(created.getId());
        assertEquals("QA Update Renamed", stored.getNamaPelanggan());
        assertEquals("Jl. Baru 1", stored.getAlamat());
        assertEquals("081200001111", stored.getNoHp());
    }

    @Test
    @DisplayName("update() succeeds repeatedly on the same row")
    void repeatedUpdates() {
        TbPelangganEntity created = createCustomer("QU1002B");

        for (int i = 1; i <= 3; i++) {
            PelangganDto edited = mapper.toDto(reload(created.getId()));
            edited.setKota("Kota " + i);
            pelangganService.update(created.getId(), mapper.toEntity(edited));
            assertEquals("Kota " + i, reload(created.getId()).getKota());
        }
    }
}
