package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.helper.PageHelper;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbMerkKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbMerkKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ApplicationScoped
public class TbKendaraanService extends AbstractCrudService<TbKendaraanEntity, Long> {

    @Inject
    TbKendaraanRepository repository;

    @Inject
    TbPelangganKendaraanRepository pelangganKendaraanRepository;

    @Inject
    TbMerkKendaraanRepository merkRepository;

    @Override
    protected PanacheRepositoryBase<TbKendaraanEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbKendaraanEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    public PageResponse<TbKendaraanEntity> findPaginated(PageRequest pageRequest) {
        return findPaginated(pageRequest, null);
    }

    public PageResponse<TbKendaraanEntity> findPaginated(PageRequest pageRequest, Long merkId) {
        boolean hasSearch = pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty();
        String like = hasSearch ? "%" + pageRequest.getSearch().toLowerCase() + "%" : null;
        if (merkId != null && hasSearch) {
            return PageHelper.paginate(repository, pageRequest,
                    "merkId = ?1 and (lower(jenis) like ?2 or lower(model) like ?2)", merkId, like);
        }
        if (merkId != null) {
            return PageHelper.paginate(repository, pageRequest, "merkId = ?1", merkId);
        }
        if (hasSearch) {
            return PageHelper.paginate(repository, pageRequest,
                    "lower(jenis) like ?1 or lower(merk) like ?1", like);
        }
        return PageHelper.findAll(repository, pageRequest);
    }

    @Override
    @Transactional
    public TbKendaraanEntity create(TbKendaraanEntity entity) {
        applyMerk(entity);
        return super.create(entity);
    }

    @Override
    @Transactional
    public TbKendaraanEntity update(Long id, TbKendaraanEntity entity) {
        applyMerk(entity);
        return super.update(id, entity);
    }

    /** Resolves merkId against the merk master and syncs the deprecated merk string. */
    private void applyMerk(TbKendaraanEntity entity) {
        if (entity.getMerkId() == null) {
            throw new IllegalArgumentException("Merk harus dipilih");
        }
        TbMerkKendaraanEntity merk = merkRepository.findByIdOptional(entity.getMerkId())
                .orElseThrow(() -> new EntityNotFoundException("Merk kendaraan not found with id: " + entity.getMerkId()));
        entity.setMerk(merk.getNama());
    }

    public List<String> findDistinctMerks() {
        return repository.findDistinctMerk();
    }

    public List<String> findDistinctJenis() {
        return repository.findDistinctJenis();
    }

    public List<String> findDistinctJenisByMerk(String merk) {
        return repository.findDistinctJenisByMerk(merk);
    }

    public TbKendaraanEntity findOrCreateByMerkJenis(String merk, String jenis) {
        return repository.findByMerkAndJenis(merk, jenis).orElseGet(() -> {
            TbMerkKendaraanEntity merkMaster = merkRepository.findOrCreateByNama(merk);
            TbKendaraanEntity entity = new TbKendaraanEntity();
            entity.setMerkId(merkMaster.getId());
            entity.setMerk(merkMaster.getNama());
            entity.setJenis(jenis == null ? "" : jenis.trim());
            return repository.getEntityManager().merge(entity);
        });
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // Check if kendaraan is referenced in tb_pelanggan_kendaraan
        List<TbPelangganKendaraanEntity> references = pelangganKendaraanRepository.findByKendaraanIdOrderByTanggalMulai(id);
        
        if (!references.isEmpty()) {
            log.warn("Cannot delete kendaraan with id: {} - referenced in {} tb_pelanggan_kendaraan records", id, references.size());
            throw new IllegalStateException("Cannot delete kendaraan - it is still referenced in " + references.size() + " pelanggan_kendaraan records. Please delete the references first.");
        }
        
        // Safe to delete if no references
        super.delete(id);
    }
}
