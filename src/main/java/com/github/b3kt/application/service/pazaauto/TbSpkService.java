package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class TbSpkService extends AbstractCrudService<TbSpkEntity, Long> {

    @Inject
    TbSpkRepository repository;

    @Inject
    TbKaryawanRepository karyawanRepository;

    @Inject
    TbPelangganService pelangganService;

    @Inject
    com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkDetailRepository detailRepository;

    @Override
    protected PanacheRepositoryBase<TbSpkEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbSpkEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    @jakarta.transaction.Transactional
    public TbSpkEntity create(TbSpkEntity entity) {
        // Persist SPK first to get ID/NoSPK if needed (though NoSPK seems
        if (entity.getNoAntrian() == null) {
            final int noAntrian = Integer.parseInt(entity.getNoSpk().substring(entity.getNoSpk().length() - 2));
            entity.setNoAntrian(noAntrian);
        }

        // pre-generated)
        super.create(entity);

        // Save details
        saveDetails(entity);

        return entity;
    }

    @Override
    @jakarta.transaction.Transactional
    public TbSpkEntity update(Long id, TbSpkEntity entity) {
        TbSpkEntity updated = super.update(id, entity);

        // Delete existing details
        detailRepository.delete("id.noSpk", updated.getNoSpk());

        // update status
        if (entity.isStartProcess()) {
            updated.setStatusSpk("PROSES");
            updated.setStartedAt(LocalDateTime.now());
        }

        // Save new details
        saveDetails(entity);

        return updated;
    }

    @Override
    public TbSpkEntity findById(Long id) {
        TbSpkEntity entity = super.findById(id);
        if (entity != null) {
            List<com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity> details = detailRepository
                    .find("id.noSpk", entity.getNoSpk()).list();
            entity.setDetails(details);
        }
        return entity;
    }

    private void saveDetails(TbSpkEntity entity) {
        if (entity.getDetails() != null) {
            for (com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity detail : entity
                    .getDetails()) {
                if (detail.getId() == null) {
                    detail.setId(new com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailId());
                }
                detail.getId().setNoSpk(entity.getNoSpk());
                // Ensure namaJasa is set from frontend or logic
                if (detail.getId().getNamaJasa() == null || detail.getId().getNamaJasa().isEmpty()) {
                    // Fallback or error? Assuming frontend sends it.
                    // If it's a Barang, we might need to use namaBarang as namaJasa for the key?
                    // The requirement says "Detail SPK can be TbJasaEntity / TbBarangEntity"
                    // But TbSpkDetailId has 'namaJasa'.
                    // We'll assume the frontend puts the item name in 'namaJasa' of the ID.
                }
                detailRepository.persist(detail);
            }
        }
    }

    @Override
    public PageResponse<TbSpkEntity> findPaginated(PageRequest pageRequest) {
        PanacheQuery<TbSpkEntity> query;

        // Build query with filters
        String queryString = "1=1";
        Object[] params = new Object[0];
        int paramIndex = 1;

        // Apply search filter if specified
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
            queryString += " and (lower(noSpk) like ?" + paramIndex + " or lower(nopol) like ?" + paramIndex
                    + " or lower(namaKaryawan) like ?" + paramIndex + ")";
            params = new Object[] { searchPattern };
            paramIndex++;
        }

        // Apply status filter if specified
        if (pageRequest.getStatusFilter() != null && !pageRequest.getStatusFilter().isEmpty()) {
            String[] statuses = pageRequest.getStatusFilter().split(",");
            StringBuilder statusQuery = new StringBuilder(" and (");
            Object[] newParams = new Object[params.length + statuses.length];
            System.arraycopy(params, 0, newParams, 0, params.length);

            for (int i = 0; i < statuses.length; i++) {
                if (i > 0) {
                    statusQuery.append(" or ");
                }
                statusQuery.append("statusSpk = ?").append(paramIndex);
                newParams[params.length + i] = statuses[i].trim().toUpperCase();
                paramIndex++;
            }
            statusQuery.append(")");
            queryString += statusQuery.toString();
            params = newParams;
        }

        // Apply today filter if specified
        if (pageRequest.isFilterToday()) {
            queryString += " and tanggalJamSpk like ?" + paramIndex;
            Object[] newParams = new Object[params.length + 1];
            System.arraycopy(params, 0, newParams, 0, params.length);
            newParams[params.length] = java.time.LocalDate.now().toString() + "%";
            params = newParams;
            paramIndex++;
        }

        // Create query
        if (params.length > 0) {
            query = repository.find(queryString, params);
        } else {
            query = repository.find(queryString);
        }

        // Apply sorting if specified
        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            Sort sort = pageRequest.isDescending()
                    ? Sort.descending(pageRequest.getSortBy())
                    : Sort.ascending(pageRequest.getSortBy());

            // Re-apply query with sorting
            if (params.length > 0) {
                query = repository.find(queryString, sort, params);
            } else {
                query = repository.find(queryString, sort);
            }
        }

        // Get total count
        long totalCount = query.count();

        // Apply pagination
        List<TbSpkEntity> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage())).list();

        fillRequiredFields(rows);

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }

    private void fillRequiredFields(List<TbSpkEntity> entities) {

        entities
                .stream()
                .forEach(entity -> {
                    if (entity.getMekanikList() != null) {
                        List<Long> ids = entity.getMekanikList().stream().map(SpkMekanik::getId)
                                .collect(Collectors.toList());

                        List<String> names = karyawanRepository.find("id in :ids", Parameters.with("ids", ids)).stream()
                                .map(obj -> obj.getNamaKaryawan()).collect(Collectors.toList());

                        entity.setNamaKaryawan(String.join(", ", names));
                    }

                    if (entity.getNopol() != null) {
                        Optional.ofNullable(pelangganService.findByNopol(entity.getNopol()))
                                .ifPresent(pelanggan -> {
                                    entity.setPelangganId(pelanggan.getId());
                                    entity.setNamaPelanggan(pelanggan.getNamaPelanggan());
                                });
                    }
                });

    }

    public String getNextSpkNumber(String spkNumber) {
        final String spkPattern = spkNumber + "%";
        return repository
                .find("where noSpk LIKE :spkPattern order by id desc", Parameters.with("spkPattern", spkPattern))
                .list()
                .stream()
                .map(TbSpkEntity::getNoSpk)
                .findFirst()
                .orElse(spkNumber + "00");
    }
}
