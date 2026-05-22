package com.github.b3kt.infrastructure.repository;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.helper.PageHelper;
import com.github.b3kt.domain.model.pazaauto.Pelanggan;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import io.quarkus.cache.CacheResult;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JpaPelangganRepository implements PelangganRepository {

    @Inject
    TbPelangganRepository panacheRepository;

    @Inject
    @io.quarkus.cache.CacheName("pelanggan-by-nopol")
    io.quarkus.cache.Cache cache;

    @Override
    public Optional<Pelanggan> findById(Long id) {
        return panacheRepository.findByIdOptional(id)
                .map(TbPelangganEntity::toDomain);
    }

    @Override
    @CacheResult(cacheName = "pelanggan-by-nopol")
    public Optional<Pelanggan> findByNopol(String nopol) {
        if (nopol == null || nopol.trim().isEmpty()) {
            return Optional.empty();
        }
        return panacheRepository.find("nopol", Sort.descending("updatedAt"), nopol)
                .firstResultOptional()
                .map(TbPelangganEntity::toDomain);
    }

    @Override
    @jakarta.transaction.Transactional
    public Pelanggan save(Pelanggan pelanggan) {
        if (pelanggan.getId() != null) {
            panacheRepository.findByIdOptional(pelanggan.getId()).ifPresent(old -> {
                if (!old.getNopol().equals(pelanggan.getNopol())) {
                    cache.invalidate(old.getNopol()).await().indefinitely();
                }
            });
        }
        if (pelanggan.getNopol() != null) {
            cache.invalidate(pelanggan.getNopol()).await().indefinitely();
        }
        TbPelangganEntity entity = TbPelangganEntity.fromDomain(pelanggan);
        TbPelangganEntity managed = panacheRepository.getEntityManager().merge(entity);
        return managed.toDomain();
    }

    @Override
    @jakarta.transaction.Transactional
    public void deleteById(Long id) {
        panacheRepository.deleteById(id);
    }

    @Override
    public List<Pelanggan> findAll() {
        return panacheRepository.findAll()
                .stream()
                .map(TbPelangganEntity::toDomain)
                .toList();
    }

    @Override
    public PageResponse<Pelanggan> findPaginated(PageRequest pageRequest) {
        PageResponse<TbPelangganEntity> entityPage;
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            entityPage = PageHelper.paginate(panacheRepository, pageRequest,
                    "lower(namaPelanggan) like ?1 or lower(nopol) like ?1 or lower(email) like ?1",
                    "%" + pageRequest.getSearch().toLowerCase() + "%");
        } else {
            entityPage = PageHelper.findAll(panacheRepository, pageRequest);
        }
        List<Pelanggan> rows = entityPage.getRows().stream()
                .map(TbPelangganEntity::toDomain)
                .toList();
        return new PageResponse<>(rows, entityPage.getPage(), entityPage.getRowsPerPage(), entityPage.getRowsNumber());
    }
}
