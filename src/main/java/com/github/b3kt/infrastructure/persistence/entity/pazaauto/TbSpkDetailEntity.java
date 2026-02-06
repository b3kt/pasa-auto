package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_spk_detail")
public class TbSpkDetailEntity {

    @EmbeddedId
    private TbSpkDetailId id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "jumlah")
    private Integer jumlah;

    @Column(name = "keterangan", length = 500)
    private String keterangan;

    @Column(name = "id_jasa")
    private Long jasaId;

    @Column(name = "id_sparepart")
    private Long sparepartId;

    public TbSpkDetailId getId() {
        return id;
    }

    public void setId(TbSpkDetailId id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getJumlah() {
        return jumlah;
    }

    public void setJumlah(Integer jumlah) {
        this.jumlah = jumlah;
    }

    public String getKeterangan() {
        return keterangan;
    }

    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    public Long getJasaId() {
        return jasaId;
    }

    public void setJasaId(Long jasaId) {
        this.jasaId = jasaId;
    }

    public Long getSparepartId() {
        return sparepartId;
    }

    public void setSparepartId(Long sparepartId) {
        this.sparepartId = sparepartId;
    }
}

