package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbBarangEntity;
import com.github.b3kt.integration.IntegrationTestBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AbstractCrudService.update() is the shared update path for every
 * @Version-ed entity in the app (19 services), but none of their DTOs carry
 * the version through to the client, so a freshly mapped entity always has
 * version == null. Regression test for the resulting
 * "Row was already updated or deleted by another transaction" on every
 * update.
 * <p>
 * Note this fix trades true optimistic locking for last-write-wins: since the
 * API never gave clients a version to send back, there was no real conflict
 * detection to preserve (every update failed unconditionally before this
 * fix). update() now always re-reads and stamps the row's current version
 * immediately before merge, so a write that lost a race with another request
 * committed in between is silently applied rather than rejected.
 */
@QuarkusTest
class AbstractCrudServiceVersionIntegrationTest extends IntegrationTestBase {

    @Inject
    TbBarangService barangService;

    @Test
    @DisplayName("update() with an entity built from a version-less DTO does not throw a stale-row error")
    void testUpdate_entityWithNullVersion_succeeds() {
        TbBarangEntity created = new TbBarangEntity();
        created.setNamaBarang("Oli Mesin");
        created.setHargaJual(BigDecimal.valueOf(50000));
        TbBarangEntity persisted = barangService.create(created);

        // Mirrors what a DTO-mapped entity looks like: version is never set.
        TbBarangEntity update = new TbBarangEntity();
        update.setNamaBarang("Oli Mesin Sintetik");
        update.setHargaJual(BigDecimal.valueOf(65000));

        TbBarangEntity updated = barangService.update(persisted.getId(), update);

        assertEquals("Oli Mesin Sintetik", updated.getNamaBarang());
        assertEquals(0, BigDecimal.valueOf(65000).compareTo(updated.getHargaJual()));
    }

    @Test
    @DisplayName("update() succeeds again on the same row (version keeps advancing correctly)")
    void testUpdate_repeatedUpdates_eachSucceeds() {
        TbBarangEntity created = new TbBarangEntity();
        created.setNamaBarang("Kampas Rem");
        created.setHargaJual(BigDecimal.valueOf(80000));
        TbBarangEntity persisted = barangService.create(created);

        TbBarangEntity firstUpdate = new TbBarangEntity();
        firstUpdate.setNamaBarang("Kampas Rem Depan");
        firstUpdate.setHargaJual(BigDecimal.valueOf(85000));
        barangService.update(persisted.getId(), firstUpdate);

        TbBarangEntity secondUpdate = new TbBarangEntity();
        secondUpdate.setNamaBarang("Kampas Rem Depan Premium");
        secondUpdate.setHargaJual(BigDecimal.valueOf(95000));
        TbBarangEntity result = barangService.update(persisted.getId(), secondUpdate);

        assertEquals("Kampas Rem Depan Premium", result.getNamaBarang());
    }
}
