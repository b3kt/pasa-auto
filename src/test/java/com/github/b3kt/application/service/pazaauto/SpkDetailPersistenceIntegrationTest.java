package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.pazaauto.SpkDetailDto;
import com.github.b3kt.application.dto.pazaauto.SpkDto;
import com.github.b3kt.application.mapper.pazaauto.SpkMapper;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik;
import com.github.b3kt.integration.IntegrationTestBase;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Reproduces the real POST /api/pazaauto/spk flow (mapper + service + real
 * Hibernate session) for an SPK with more than one detail line. Regression
 * test for the NonUniqueObjectException reported when saving an SPK with a
 * jasa line and a sparepart line: the frontend sends each detail's key under
 * a flat "namaItem" field, which SpkDetailDto/SpkDetailMapper previously
 * dropped, leaving every detail's embedded id (no_spk, nama_jasa) collide on
 * nama_jasa = null within the same persistence context.
 */
@QuarkusTest
class SpkDetailPersistenceIntegrationTest extends IntegrationTestBase {

    @Inject
    TbSpkService spkService;

    @Inject
    SpkMapper spkMapper;

    @Inject
    SpkDetailService spkDetailService;

    @Test
    @DisplayName("create() with a jasa line and a sparepart line persists both distinct detail rows")
    void testCreate_multipleDetails_persistsDistinctRows() {
        SpkMekanik mekanik = new SpkMekanik();
        mekanik.setTugas("Utama");

        SpkDto dto = SpkDto.builder()
                .noSpk("IT20260918")
                .noAntrian(1)
                .tanggalJamSpk("2026-09-18 04:10")
                .nopol("B 1019 CYM")
                .namaKaryawan("")
                .km(1)
                .statusSpk("OPEN")
                .keterangan("")
                .status("")
                .namaPelanggan("KO LIM")
                .alamatPelanggan("")
                .merkKendaraan("TOYOTA")
                .jenisKendaraan("CALYA")
                .mekanikList(List.of(mekanik))
                .details(List.of(
                        SpkDetailDto.builder()
                                .namaItem("SCANER AIR BAG")
                                .jasaId(181L)
                                .harga(BigDecimal.valueOf(150000))
                                .hargaMaster(BigDecimal.valueOf(150000))
                                .jumlah(1)
                                .keterangan("")
                                .build(),
                        SpkDetailDto.builder()
                                .namaItem("AKI MASSIV NS60")
                                .sparepartId(31L)
                                .harga(BigDecimal.valueOf(570000))
                                .hargaMaster(BigDecimal.valueOf(570000))
                                .jumlah(1)
                                .keterangan("")
                                .build()
                ))
                .build();

        TbSpkEntity entity = spkMapper.toEntity(dto);

        TbSpkEntity created = spkService.create(entity);

        List<TbSpkDetailEntity> saved = spkDetailService.findByNoSpk(created.getNoSpk());
        assertEquals(2, saved.size(), "both detail rows should be persisted");
        assertNotEquals(saved.get(0).getId(), saved.get(1).getId(), "detail rows must have distinct composite ids");
        assertEquals("IT20260918", saved.get(0).getId().getNoSpk());
    }

    @Test
    @DisplayName("PUT update then GET reproduces the reported save flow through the real REST layer")
    void testUpdateThenGet_matchesReportedPayload() {
        SpkMekanik mekanik = new SpkMekanik();
        mekanik.setTugas("Utama");

        SpkDto createDto = SpkDto.builder()
                .noSpk("IT20260919")
                .noAntrian(1)
                .tanggalJamSpk("2026-09-18 04:10")
                .nopol("B 1019 CYM")
                .namaKaryawan("")
                .km(1)
                .statusSpk("OPEN")
                .keterangan("")
                .status("")
                .namaPelanggan("KO LIM")
                .mekanikList(List.of(mekanik))
                .details(List.of())
                .build();
        TbSpkEntity created = spkService.create(spkMapper.toEntity(createDto));

        // Mirrors the exact PUT body from the bug report (mekanikList with existing
        // ids, two detail lines using namaItem, no version field on the DTO).
        Map<String, Object> putBody = new LinkedHashMap<>();
        putBody.put("id", created.getId());
        putBody.put("noSpk", created.getNoSpk());
        putBody.put("noAntrian", 1);
        putBody.put("tanggalJamSpk", "2026-09-18 04:25");
        putBody.put("nopol", "B 1019 CYM");
        putBody.put("namaKaryawan", "");
        putBody.put("km", 1);
        putBody.put("statusSpk", "OPEN");
        putBody.put("keterangan", "");
        putBody.put("status", "");
        putBody.put("namaPelanggan", "KO LIM");
        putBody.put("mekanikList", List.of(Map.of("id", 4, "tugas", "Utama"), Map.of("id", 13, "tugas", "Utama")));
        putBody.put("details", List.of(
                Map.of("namaItem", "REPAIR SPION(GREASE 2 PCS)", "jasaId", 179,
                        "harga", 500000, "hargaMaster", 500000, "jumlah", 1, "keterangan", ""),
                Map.of("namaItem", "AKI INCOE TT", "sparepartId", 29,
                        "harga", 850000, "hargaMaster", 850000, "jumlah", 1, "keterangan", "")
        ));

        given().contentType(ContentType.JSON).body(putBody)
                .when().put("/api/pazaauto/spk/" + created.getId())
                .then().statusCode(200)
                .body("success", equalTo(true));

        given().when().get("/api/pazaauto/spk/" + created.getId())
                .then().statusCode(200)
                .body("success", equalTo(true));
    }
}
