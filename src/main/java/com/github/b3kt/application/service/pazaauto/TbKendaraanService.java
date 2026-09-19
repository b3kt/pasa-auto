package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.helper.PageHelper;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            return PageHelper.paginate(repository, pageRequest,
                    "lower(jenis) like ?1 or lower(merk) like ?1",
                    "%" + pageRequest.getSearch().toLowerCase() + "%");
        }
        return PageHelper.findAll(repository, pageRequest);
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
            TbKendaraanEntity entity = new TbKendaraanEntity();
            entity.setMerk(merk);
            entity.setJenis(jenis);
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
