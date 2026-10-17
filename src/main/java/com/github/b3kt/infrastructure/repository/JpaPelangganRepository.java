package com.github.b3kt.infrastructure.repository;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.helper.NopolNormalizer;
import com.github.b3kt.application.helper.PageHelper;
import com.github.b3kt.domain.model.pazaauto.Pelanggan;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JpaPelangganRepository implements PelangganRepository {

    @Inject
    TbPelangganRepository panacheRepository;

    @Inject
    TbPelangganKendaraanRepository ownershipRepository;

    @Inject
    @io.quarkus.cache.CacheName("pelanggan-by-nopol")
    io.quarkus.cache.Cache cache;

    @Override
    public Optional<Pelanggan> findById(Long id) {
        return panacheRepository.findByIdOptional(id)
                .map(TbPelangganEntity::toDomain);
    }

    @Override
    public Optional<Pelanggan> findByNopol(String nopol) {
        String norm = NopolNormalizer.normalize(nopol);
        if (norm.isEmpty()) {
            return Optional.empty();
        }
        // Cache keyed on the canonical plate so 'B 1019 CYM' and 'B1019CYM' share one entry,
        // and ownership-write invalidation can target a single key.
        return cache.get(norm, key -> resolveByNopol(norm)).await().indefinitely();
    }

    private Optional<Pelanggan> resolveByNopol(String norm) {
        // The ownership junction is the only source: since V35 every legacy plate has an ownership
        // row and tb_pelanggan.nopol is derived from it, so there is nothing left to fall back to.
        return ownershipRepository.findCurrentByNopol(norm)
                .map(TbPelangganKendaraanEntity::getPelangganId)
                .flatMap(panacheRepository::findByIdOptional)
                .map(TbPelangganEntity::toDomain);
    }

    /**
     * Evict cached nopol lookups. Called whenever vehicle ownership changes (attach, transfer,
     * detach, master change) so the ownership-based resolution does not serve stale owners.
     */
    public void invalidateByNopol(String... nopols) {
        if (nopols == null) {
            return;
        }
        for (String nopol : nopols) {
            String norm = NopolNormalizer.normalize(nopol);
            if (!norm.isEmpty()) {
                cache.invalidate(norm).await().indefinitely();
            }
        }
    }

    @Override
    @jakarta.transaction.Transactional
    public Pelanggan save(Pelanggan pelanggan) {
        String previousNopol = null;
        if (pelanggan.getId() != null) {
            previousNopol = panacheRepository.findByIdOptional(pelanggan.getId())
                    .map(TbPelangganEntity::getNopol)
                    .orElse(null);
        }
        invalidateByNopol(previousNopol, pelanggan.getNopol());
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
            // Plates are matched against the customer's current vehicles, in canonical form.
            entityPage = PageHelper.paginate(panacheRepository, pageRequest,
                    "lower(namaPelanggan) like ?1 or lower(email) like ?1 or id in ("
                            + "select k.pelangganId from TbPelangganKendaraanEntity k "
                            + "where k.nopol like ?2 and k.current = true and k.deletedAt is null)",
                    "%" + pageRequest.getSearch().toLowerCase() + "%",
                    "%" + NopolNormalizer.normalize(pageRequest.getSearch()) + "%");
        } else {
            entityPage = PageHelper.findAll(panacheRepository, pageRequest);
        }
        List<Pelanggan> rows = entityPage.getRows().stream()
                .map(TbPelangganEntity::toDomain)
                .toList();
        return new PageResponse<>(rows, entityPage.getPage(), entityPage.getRowsPerPage(), entityPage.getRowsNumber());
    }
}
