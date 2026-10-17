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

    @Inject
    PelangganOwnershipSyncService ownershipSyncService;

    @Inject
    PelangganVehicleResolver vehicleResolver;

    @Inject
    PelangganContactResolver contactResolver;

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
        List<TbPelangganEntity> rows = pelangganRepository.findAll()
                .stream()
                .map(TbPelangganEntity::fromDomain)
                .toList();
        rows.forEach(vehicleResolver::enrichVehicleInfo);
        contactResolver.applyTo(rows);
        return rows;
    }

    @Override
    public TbPelangganEntity findById(Long id) {
        TbPelangganEntity entity = pelangganRepository.findById(id)
                .map(TbPelangganEntity::fromDomain)
                .orElse(null);
        enrich(entity);
        return entity;
    }

    @Override
    public PageResponse<TbPelangganEntity> findPaginated(PageRequest pageRequest) {
        PageResponse<Pelanggan> domainPage = pelangganRepository.findPaginated(pageRequest);
        List<TbPelangganEntity> rows = domainPage.getRows()
                .stream()
                .map(TbPelangganEntity::fromDomain)
                .toList();
        rows.forEach(vehicleResolver::enrichVehicleInfo);
        contactResolver.applyTo(rows);
        return new PageResponse<>(rows, domainPage.getPage(), domainPage.getRowsPerPage(), domainPage.getRowsNumber());
    }

    @Override
    @jakarta.transaction.Transactional
    public TbPelangganEntity create(TbPelangganEntity entity) {
        Pelanggan domain = entity.toDomain();
        Pelanggan saved = pelangganRepository.save(domain);
        ownershipSyncService.syncOnCreate(saved.getId(), saved.getNopol(), saved.getMerk(),
                saved.getJenis(), saved.getTanggalJoin());
        return enrich(TbPelangganEntity.fromDomain(saved));
    }

    @Override
    @jakarta.transaction.Transactional
    public TbPelangganEntity update(Long id, TbPelangganEntity entity) {
        Pelanggan domain = pelangganRepository.findById(id).orElse(null);
        if (domain == null) {
            return null;
        }
        String oldNopol = domain.getNopol();
        String oldMerk = domain.getMerk();
        String oldJenis = domain.getJenis();
        domain.setNopol(entity.getNopol());
        domain.setNamaPelanggan(entity.getNamaPelanggan());
        domain.setAlamat(entity.getAlamat());
        domain.setContactPerson(entity.getContactPerson());
        domain.setMerk(entity.getMerk());
        domain.setJenis(entity.getJenis());
        domain.setJenisKelamin(entity.getJenisKelamin());
        domain.setKeterangan(entity.getKeterangan());
        domain.setKodePos(entity.getKodePos());
        domain.setKota(entity.getKota());
        domain.setTanggalJoin(entity.getTanggalJoin());
        // A linked customer's phone/email belong to their portal account (ADR-0012).
        if (!contactResolver.isManagedByAccount(id)) {
            domain.setNoHp(entity.getNoHp());
            domain.setEmail(entity.getEmail());
        }
        Pelanggan saved = pelangganRepository.save(domain);
        ownershipSyncService.syncOnUpdate(saved.getId(), oldNopol, oldMerk, oldJenis,
                saved.getNopol(), saved.getMerk(), saved.getJenis(), saved.getTanggalJoin());
        return enrich(TbPelangganEntity.fromDomain(saved));
    }

    @Override
    @jakarta.transaction.Transactional
    public void delete(Long id) {
        pelangganRepository.deleteById(id);
    }

    public TbPelangganEntity findByNopol(String nopol) {
        TbPelangganEntity entity = pelangganRepository.findByNopol(nopol)
                .map(TbPelangganEntity::fromDomain)
                .orElse(null);
        enrich(entity);
        return entity;
    }

    @jakarta.transaction.Transactional
    public TbPelangganEntity patchByNopol(String nopol, TbPelangganEntity data) {
        Pelanggan domain = pelangganRepository.findByNopol(nopol).orElse(null);
        if (domain == null) return null;

        String oldMerk = domain.getMerk();
        String oldJenis = domain.getJenis();
        if (data.getNamaPelanggan() != null) domain.setNamaPelanggan(data.getNamaPelanggan());
        if (data.getAlamat() != null) domain.setAlamat(data.getAlamat());
        if (data.getMerk() != null) domain.setMerk(data.getMerk());
        if (data.getJenis() != null) domain.setJenis(data.getJenis());
        if (data.getContactPerson() != null) domain.setContactPerson(data.getContactPerson());
        boolean contactFromAccount = contactResolver.isManagedByAccount(domain.getId());
        if (data.getEmail() != null && !contactFromAccount) domain.setEmail(data.getEmail());
        if (data.getJenisKelamin() != null) domain.setJenisKelamin(data.getJenisKelamin());
        if (data.getKeterangan() != null) domain.setKeterangan(data.getKeterangan());
        if (data.getKodePos() != null) domain.setKodePos(data.getKodePos());
        if (data.getKota() != null) domain.setKota(data.getKota());
        if (data.getNoHp() != null && !contactFromAccount) domain.setNoHp(data.getNoHp());

        Pelanggan saved = pelangganRepository.save(domain);
        ownershipSyncService.syncOnUpdate(saved.getId(), saved.getNopol(), oldMerk, oldJenis,
                saved.getNopol(), saved.getMerk(), saved.getJenis(), saved.getTanggalJoin());
        return enrich(TbPelangganEntity.fromDomain(saved));
    }

    /** Fills the read-model view fields (primary vehicle, effective contact) of a detached copy. */
    private TbPelangganEntity enrich(TbPelangganEntity entity) {
        if (entity != null) {
            vehicleResolver.enrichVehicleInfo(entity);
            contactResolver.applyTo(List.of(entity));
        }
        return entity;
    }
}
