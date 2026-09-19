package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.helper.PageHelper;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbMerkKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbMerkKendaraanRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class TbMerkKendaraanService extends AbstractCrudService<TbMerkKendaraanEntity, Long> {

    @Inject
    TbMerkKendaraanRepository repository;

    @Inject
    TbKendaraanRepository kendaraanRepository;

    @Override
    protected PanacheRepositoryBase<TbMerkKendaraanEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbMerkKendaraanEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    public PageResponse<TbMerkKendaraanEntity> findPaginated(PageRequest pageRequest) {
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            return PageHelper.paginate(repository, pageRequest,
                    "lower(nama) like ?1",
                    "%" + pageRequest.getSearch().toLowerCase() + "%");
        }
        return PageHelper.findAll(repository, pageRequest);
    }

    @Override
    @Transactional
    public TbMerkKendaraanEntity create(TbMerkKendaraanEntity entity) {
        normalizeAndCheckUnique(entity, null);
        return super.create(entity);
    }

    @Override
    @Transactional
    public TbMerkKendaraanEntity update(Long id, TbMerkKendaraanEntity entity) {
        normalizeAndCheckUnique(entity, id);
        TbMerkKendaraanEntity updated = super.update(id, entity);
        // Keep the deprecated tb_kendaraan.merk string in sync with the master name.
        kendaraanRepository.update("merk = ?1 where merkId = ?2", updated.getNama(), id);
        return updated;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        long references = kendaraanRepository.count("merkId", id);
        if (references > 0) {
            log.warn("Cannot delete merk kendaraan with id: {} - referenced by {} tb_kendaraan records", id, references);
            throw new IllegalStateException("Cannot delete merk - it is still used by " + references + " kendaraan records. Please delete or move them first.");
        }
        super.delete(id);
    }

    private void normalizeAndCheckUnique(TbMerkKendaraanEntity entity, Long selfId) {
        String nama = TbMerkKendaraanRepository.normalizeNama(entity.getNama());
        if (nama == null || nama.isEmpty()) {
            throw new IllegalArgumentException("Nama merk harus diisi");
        }
        entity.setNama(nama);
        repository.findByNamaIgnoreCase(nama)
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Merk " + nama + " sudah ada");
                });
    }

    public TbMerkKendaraanEntity requireById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Merk harus dipilih");
        }
        return repository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Merk kendaraan not found with id: " + id));
    }
}
