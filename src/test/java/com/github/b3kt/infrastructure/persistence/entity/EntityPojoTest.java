package com.github.b3kt.infrastructure.persistence.entity;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EntityPojoTest {

    @Test
    @DisplayName("BaseEntity setters and getters")
    void baseEntity() {
        class TestEntity extends BaseEntity {}
        TestEntity e = new TestEntity();
        e.setId(1L);
        e.setCreatedAt(LocalDateTime.now());
        e.setCreatedBy("admin");
        e.setUpdatedAt(LocalDateTime.now());
        e.setUpdatedBy("admin");
        e.setVersion(1);
        assertEquals(1L, e.getId());
        assertEquals("admin", e.getCreatedBy());
    }

    @Test
    @DisplayName("UserEntity")
    void userEntity() {
        UserEntity e = new UserEntity();
        e.setId(1L);
        e.setUsername("testuser");
        e.setEmail("test@x.com");
        e.setPasswordHash("hash");
        e.setActive(true);
        assertEquals("testuser", e.getUsername());
        assertEquals("test@x.com", e.getEmail());
    }

    @Test
    @DisplayName("RoleEntity")
    void roleEntity() {
        RoleEntity e = new RoleEntity();
        e.setId(1L);
        e.setName("admin");
        e.setDescription("Admin role");
        e.setActive(true);
        assertEquals("admin", e.getName());
    }

    @Test
    @DisplayName("RoleEntity parameterized constructor")
    void roleEntityConstructor() {
        RoleEntity e = new RoleEntity("editor", "Editor role");
        assertEquals("editor", e.getName());
        assertTrue(e.isActive());
    }

    @Test
    @DisplayName("PermissionEntity")
    void permissionEntity() {
        PermissionEntity e = new PermissionEntity();
        e.setId(1L);
        e.setName("read:users");
        e.setResource("users");
        e.setAction("read");
        e.setActive(true);
        assertEquals("read:users", e.getName());
    }

    @Test
    @DisplayName("PermissionEntity parameterized constructor")
    void permissionEntityConstructor() {
        PermissionEntity e = new PermissionEntity("read:users", "desc", "users", "read");
        assertEquals("read:users", e.getName());
    }

    @Test
    @DisplayName("SystemParameterEntity")
    void systemParameterEntity() {
        SystemParameterEntity e = new SystemParameterEntity();
        e.setId(1L);
        e.setName("app.name");
        e.setValue("Pasa Auto");
        assertEquals("app.name", e.getName());
    }

    @Test
    @DisplayName("AuditTrailEntity")
    void auditTrailEntity() {
        AuditTrailEntity e = new AuditTrailEntity();
        e.setId(1L);
        e.setTableName("users");
        e.setRecordId(1L);
        e.setAction("CREATE");
        e.setUsername("admin");
        e.setTimestamp(LocalDateTime.now());
        assertEquals("users", e.getTableName());
    }

    @Test
    @DisplayName("TbSpkEntity")
    void tbSpkEntity() {
        TbSpkEntity e = new TbSpkEntity();
        e.setId(1L);
        e.setNoSpk("SPK001");
        e.setNopol("B1234XYZ");
        e.setStatusSpk("MENUNGGU");
        assertEquals("SPK001", e.getNoSpk());
    }

    @Test
    @DisplayName("TbSpkDetailEntity")
    void tbSpkDetailEntity() {
        TbSpkDetailEntity e = new TbSpkDetailEntity();
        TbSpkDetailId id = new TbSpkDetailId("SPK001", "Oli");
        e.setId(id);
        e.setHarga(BigDecimal.valueOf(50000));
        e.setJumlah(2);
        assertEquals("SPK001", e.getId().getNoSpk());
        assertEquals("Oli", e.getId().getNamaJasa());
    }

    @Test
    @DisplayName("TbPenjualanEntity")
    void tbPenjualanEntity() {
        TbPenjualanEntity e = new TbPenjualanEntity();
        e.setId(1L);
        e.setNoPenjualan("PJ001");
        assertEquals("PJ001", e.getNoPenjualan());
    }

    @Test
    @DisplayName("TbPenjualanDetailEntity")
    void tbPenjualanDetailEntity() {
        TbPenjualanDetailEntity e = new TbPenjualanDetailEntity();
        e.setId(1L);
        e.setNamaJasaBarang("Oli");
        e.setHargaJual(50000);
        assertEquals("Oli", e.getNamaJasaBarang());
        assertEquals(50000, e.getHargaJual());
    }

    @Test
    @DisplayName("TbPembelianEntity")
    void tbPembelianEntity() {
        TbPembelianEntity e = new TbPembelianEntity();
        e.setId(1L);
        e.setNoPembelian("FS001");
        e.setJenisPembelian("SPAREPART");
        assertEquals("FS001", e.getNoPembelian());
        assertEquals("SPAREPART", e.getJenisPembelian());
    }

    @Test
    @DisplayName("TbPembelianDetailEntity")
    void tbPembelianDetailEntity() {
        TbPembelianDetailEntity e = new TbPembelianDetailEntity();
        e.setId(1L);
        e.setKeterangan("Filter");
        assertEquals("Filter", e.getKeterangan());
    }

    @Test
    @DisplayName("TbBarangEntity")
    void tbBarangEntity() {
        TbBarangEntity e = new TbBarangEntity();
        e.setId(1L);
        e.setNamaBarang("Oli Mesin");
        assertEquals("Oli Mesin", e.getNamaBarang());
    }

    @Test
    @DisplayName("TbJasaEntity")
    void tbJasaEntity() {
        TbJasaEntity e = new TbJasaEntity();
        e.setId(1L);
        e.setNamaJasa("Service Ringan");
        assertEquals("Service Ringan", e.getNamaJasa());
    }

    @Test
    @DisplayName("TbKaryawanEntity")
    void tbKaryawanEntity() {
        TbKaryawanEntity e = new TbKaryawanEntity();
        e.setId(1L);
        e.setNamaKaryawan("Budi");
        assertEquals("Budi", e.getNamaKaryawan());
    }

    @Test
    @DisplayName("TbKaryawanPosisiEntity")
    void tbKaryawanPosisiEntity() {
        TbKaryawanPosisiEntity e = new TbKaryawanPosisiEntity();
        e.setId(1L);
        e.setPosisi("Montir");
        assertEquals("Montir", e.getPosisi());
    }

    @Test
    @DisplayName("TbKendaraanEntity")
    void tbKendaraanEntity() {
        TbKendaraanEntity e = new TbKendaraanEntity();
        e.setId(1L);
        e.setMerk("Honda");
        e.setJenis("Mobil");
        assertEquals("Honda", e.getMerk());
    }

    @Test
    @DisplayName("TbPelangganEntity")
    void tbPelangganEntity() {
        TbPelangganEntity e = new TbPelangganEntity();
        e.setId(1L);
        e.setNamaPelanggan("Agus");
        e.setNopol("B1234CD");
        assertEquals("Agus", e.getNamaPelanggan());
    }

    @Test
    @DisplayName("TbSparepartEntity")
    void tbSparepartEntity() {
        TbSparepartEntity e = new TbSparepartEntity();
        e.setId(1L);
        e.setNamaSparepart("Filter Oli");
        assertEquals("Filter Oli", e.getNamaSparepart());
    }

    @Test
    @DisplayName("TbSupplierEntity")
    void tbSupplierEntity() {
        TbSupplierEntity e = new TbSupplierEntity();
        e.setId(1);
        e.setNamaSupplier("PT Sukses");
        assertEquals("PT Sukses", e.getNamaSupplier());
    }

    @Test
    @DisplayName("TbAbsensiEntity")
    void tbAbsensiEntity() {
        TbAbsensiEntity e = new TbAbsensiEntity();
        e.setId(1L);
        e.setStatus("HADIR");
        assertEquals("HADIR", e.getStatus());
    }

    @Test
    @DisplayName("TbAbsensiConfigEntity")
    void tbAbsensiConfigEntity() {
        TbAbsensiConfigEntity e = new TbAbsensiConfigEntity();
        e.setId(1L);
        e.setConfigKey("jam_masuk");
        e.setConfigValue("08:00");
        assertEquals("jam_masuk", e.getConfigKey());
    }

    @Test
    @DisplayName("Enums have expected values")
    void enums() {
        assertEquals(2, JenisPembelian.values().length);
        assertEquals(3, KategoriItem.values().length);
        assertEquals(5, KategoriOperasional.values().length);

        assertEquals("SPAREPART", JenisPembelian.SPAREPART.name());
        assertEquals("SPAREPART", KategoriItem.SPAREPART.name());
        assertEquals("DAILY", KategoriOperasional.DAILY.name());
        assertEquals("Harian", KategoriOperasional.DAILY.getLabel());
    }
}
