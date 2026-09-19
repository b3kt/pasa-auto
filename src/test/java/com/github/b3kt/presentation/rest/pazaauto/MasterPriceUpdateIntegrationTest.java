package com.github.b3kt.presentation.rest.pazaauto;

import com.github.b3kt.application.service.pazaauto.TbBarangService;
import com.github.b3kt.application.service.pazaauto.TbJasaService;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbBarangEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbJasaEntity;
import com.github.b3kt.integration.IntegrationTestBase;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Mirrors the SPK "Update Keduanya" flow: the UI takes a master row as returned by
 * GET /api/pazaauto/{jasa|barang}, changes only the price and PUTs it back.
 */
@QuarkusTest
class MasterPriceUpdateIntegrationTest extends IntegrationTestBase {

    @Inject
    TbBarangService barangService;

    @Inject
    TbJasaService jasaService;

    @Test
    @TestSecurity(user = "admin", roles = "Admin")
    void updateKeduanya_barang_persistsHargaJual() {
        TbBarangEntity b = new TbBarangEntity();
        b.setNamaBarang("SCANER AIR BAG");
        b.setHargaJual(BigDecimal.valueOf(160000));
        Long id = barangService.create(b).getId();

        List<Map<String, Object>> all = given().when().get("/api/pazaauto/barang")
                .then().statusCode(200).extract().jsonPath().getList("data");
        Map<String, Object> master = new HashMap<>(all.stream()
                .filter(m -> ((Number) m.get("id")).longValue() == id).findFirst().orElseThrow());
        master.put("hargaJual", 170000);

        given().contentType(ContentType.JSON).body(master)
                .when().put("/api/pazaauto/barang/" + id)
                .then().statusCode(200).body("success", is(true));

        assertEquals(0, BigDecimal.valueOf(170000).compareTo(barangService.findById(id).getHargaJual()));
    }

    @Test
    @TestSecurity(user = "admin", roles = "Admin")
    void updateKeduanya_jasa_persistsHargaJasa() {
        TbJasaEntity j = new TbJasaEntity();
        j.setNamaJasa("SCANER AIR BAG");
        j.setHargaJasa(160000);
        Long id = jasaService.create(j).getId();

        List<Map<String, Object>> all = given().when().get("/api/pazaauto/jasa")
                .then().statusCode(200).extract().jsonPath().getList("data");
        Map<String, Object> master = new HashMap<>(all.stream()
                .filter(m -> ((Number) m.get("id")).longValue() == id).findFirst().orElseThrow());
        master.put("hargaJasa", 170000);

        given().contentType(ContentType.JSON).body(master)
                .when().put("/api/pazaauto/jasa/" + id)
                .then().statusCode(200).body("success", is(true));

        assertEquals(170000, jasaService.findById(id).getHargaJasa());
    }
}
