package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@RegisterForReflection
public class PenjualanPrintDto {
    private String noPenjualan;
    private String tanggal;
    private String noSpk;
    private String statusPembayaran;
    private String metodePembayaran;

    // Customer Info
    private String namaPelanggan;
    private String alamatPelanggan;
    private String noHpPelanggan;

    // Vehicle Info
    private String nopol;
    private String merk;
    private String model;
    private Integer km;
    private String namaMekanik;

    // Items
    private List<ItemDto> items;

    // Totals
    private BigDecimal subTotal;
    private BigDecimal diskon;
    private BigDecimal ppn;
    private Integer grandTotal;
    private BigDecimal uangDibayar;
    private BigDecimal kembalian;

    @Data
    @RegisterForReflection
    public static class ItemDto {
        private String nama;
        private Integer qty;
        private BigDecimal harga;
        private BigDecimal subTotal;
        private String type; // BARANG or JASA
    }
}
