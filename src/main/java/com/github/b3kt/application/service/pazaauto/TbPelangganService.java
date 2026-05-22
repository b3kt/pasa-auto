package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.domain.model.pazaauto.Pelanggan;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.repository.PelangganRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class TbPelangganService extends AbstractCrudService<TbPelangganEntity, Long> {

    @Inject
    PelangganRepository pelangganRepository;

    @Override
    protected PanacheRepositoryBase<TbPelangganEntity, Long> getRepository() {
        throw new UnsupportedOperationException("Use PelangganRepository instead");
    }

    @Override
    protected void setEntityId(TbPelangganEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    public List<TbPelangganEntity> findAll() {
        return pelangganRepository.findAll()
                .stream()
                .map(TbPelangganEntity::fromDomain)
                .toList();
    }

    @Override
    public TbPelangganEntity findById(Long id) {
        return pelangganRepository.findById(id)
                .map(TbPelangganEntity::fromDomain)
                .orElse(null);
    }

    @Override
    public PageResponse<TbPelangganEntity> findPaginated(PageRequest pageRequest) {
        PageResponse<Pelanggan> domainPage = pelangganRepository.findPaginated(pageRequest);
        List<TbPelangganEntity> rows = domainPage.getRows()
                .stream()
                .map(TbPelangganEntity::fromDomain)
                .toList();
        return new PageResponse<>(rows, domainPage.getPage(), domainPage.getRowsPerPage(), domainPage.getRowsNumber());
    }

    @Override
    @jakarta.transaction.Transactional
    public TbPelangganEntity create(TbPelangganEntity entity) {
        Pelanggan domain = entity.toDomain();
        Pelanggan saved = pelangganRepository.save(domain);
        return TbPelangganEntity.fromDomain(saved);
    }

    @Override
    @jakarta.transaction.Transactional
    public TbPelangganEntity update(Long id, TbPelangganEntity entity) {
        Pelanggan domain = pelangganRepository.findById(id).orElse(null);
        if (domain == null) {
            return null;
        }
        domain.setNopol(entity.getNopol());
        domain.setNamaPelanggan(entity.getNamaPelanggan());
        domain.setAlamat(entity.getAlamat());
        domain.setContactPerson(entity.getContactPerson());
        domain.setTelepon(entity.getTelepon());
        domain.setMerk(entity.getMerk());
        domain.setJenis(entity.getJenis());
        domain.setEmail(entity.getEmail());
        domain.setJenisKelamin(entity.getJenisKelamin());
        domain.setKeterangan(entity.getKeterangan());
        domain.setKodePos(entity.getKodePos());
        domain.setKota(entity.getKota());
        domain.setNoHp(entity.getNoHp());
        domain.setNoTelepon(entity.getNoTelepon());
        domain.setTanggalJoin(entity.getTanggalJoin());
        Pelanggan saved = pelangganRepository.save(domain);
        return TbPelangganEntity.fromDomain(saved);
    }

    @Override
    @jakarta.transaction.Transactional
    public void delete(Long id) {
        pelangganRepository.deleteById(id);
    }

    public TbPelangganEntity findByNopol(String nopol) {
        return pelangganRepository.findByNopol(nopol)
                .map(TbPelangganEntity::fromDomain)
                .orElse(null);
    }

    @jakarta.transaction.Transactional
    public TbPelangganEntity patchByNopol(String nopol, TbPelangganEntity data) {
        Pelanggan domain = pelangganRepository.findByNopol(nopol).orElse(null);
        if (domain == null) return null;

        if (data.getNamaPelanggan() != null) domain.setNamaPelanggan(data.getNamaPelanggan());
        if (data.getAlamat() != null) domain.setAlamat(data.getAlamat());
        if (data.getMerk() != null) domain.setMerk(data.getMerk());
        if (data.getJenis() != null) domain.setJenis(data.getJenis());
        if (data.getContactPerson() != null) domain.setContactPerson(data.getContactPerson());
        if (data.getTelepon() != null) domain.setTelepon(data.getTelepon());
        if (data.getEmail() != null) domain.setEmail(data.getEmail());
        if (data.getJenisKelamin() != null) domain.setJenisKelamin(data.getJenisKelamin());
        if (data.getKeterangan() != null) domain.setKeterangan(data.getKeterangan());
        if (data.getKodePos() != null) domain.setKodePos(data.getKodePos());
        if (data.getKota() != null) domain.setKota(data.getKota());
        if (data.getNoHp() != null) domain.setNoHp(data.getNoHp());
        if (data.getNoTelepon() != null) domain.setNoTelepon(data.getNoTelepon());

        Pelanggan saved = pelangganRepository.save(domain);
        return TbPelangganEntity.fromDomain(saved);
    }
}
