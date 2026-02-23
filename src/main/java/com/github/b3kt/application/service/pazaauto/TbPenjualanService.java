package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPenjualanRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TbPenjualanService extends AbstractCrudService<TbPenjualanEntity, String> {

    @Inject
    TbPenjualanRepository repository;

    @Override
    protected PanacheRepositoryBase<TbPenjualanEntity, String> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbPenjualanEntity entity, String id) {
        entity.setNoPenjualan(id);
    }

    public TbPenjualanEntity findByNoPenjualan(String noPenjualan) {
        return repository.findByNoPenjualan(noPenjualan);
    }

    @Transactional
    public TbPenjualanEntity createWithNoSpkValidation(TbPenjualanEntity entity) {
        // Check if noSpk already exists in penjualan
        TbPenjualanEntity existing = repository.find("noSpk", entity.getNoSpk()).firstResult();
        if (existing != null) {
            throw new IllegalArgumentException("No SPK " + entity.getNoSpk() + " already exists in penjualan records");
        }
        
        return create(entity);
    }

    @Transactional
    public TbPenjualanEntity updateWithNoSpkValidation(TbPenjualanEntity entity) {
        // Check if another record with same noSpk exists (excluding current record)
        TbPenjualanEntity existing = repository.find("noSpk = ?1 AND noPenjualan <> ?2", 
            entity.getNoSpk(), entity.getNoPenjualan()).firstResult();
        if (existing != null) {
            throw new IllegalArgumentException("No SPK " + entity.getNoSpk() + " already exists in other penjualan records");
        }
        
        return update(entity.getNoPenjualan(), entity);
    }

    @Override
    public com.github.b3kt.application.dto.PageResponse<TbPenjualanEntity> findPaginated(com.github.b3kt.application.dto.PageRequest pageRequest) {
        StringBuilder queryStr = new StringBuilder("1=1");
        io.quarkus.panache.common.Parameters params = new io.quarkus.panache.common.Parameters();

        // Search filter
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            queryStr.append(" and (lower(noPenjualan) like :search or lower(noSpk) like :search)");
            params.and("search", "%" + pageRequest.getSearch().toLowerCase() + "%");
        }

        // Status pembayaran filter
        if (pageRequest.getStatusFilter() != null && !pageRequest.getStatusFilter().isEmpty()) {
            String[] statuses = pageRequest.getStatusFilter().split(",");
            queryStr.append(" and statusPembayaran in (:statuses)");
            params.and("statuses", java.util.Arrays.asList(statuses));
        }

        // Date range filter
        if (pageRequest.getStartDate() != null && !pageRequest.getStartDate().isEmpty()) {
            // Need to parse string "YYYY-MM-DD" and set to start of day
            try {
                java.time.LocalDate startDate = java.time.LocalDate.parse(pageRequest.getStartDate());
                java.util.Date startDateTime = java.util.Date.from(startDate.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant());
                queryStr.append(" and tanggalJamPenjualan >= :startDate");
                params.and("startDate", startDateTime);
            } catch (Exception e) {}
        }
        if (pageRequest.getEndDate() != null && !pageRequest.getEndDate().isEmpty()) {
            try {
                java.time.LocalDate endDate = java.time.LocalDate.parse(pageRequest.getEndDate());
                java.util.Date endDateTime = java.util.Date.from(endDate.atTime(java.time.LocalTime.MAX).atZone(java.time.ZoneId.systemDefault()).toInstant());
                queryStr.append(" and tanggalJamPenjualan <= :endDate");
                params.and("endDate", endDateTime);
            } catch (Exception e) {}
        }

        io.quarkus.hibernate.orm.panache.PanacheQuery<TbPenjualanEntity> query = repository.find(queryStr.toString(), params);

        // Apply sorting
        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            io.quarkus.panache.common.Sort sort = pageRequest.isDescending()
                    ? io.quarkus.panache.common.Sort.descending(pageRequest.getSortBy())
                    : io.quarkus.panache.common.Sort.ascending(pageRequest.getSortBy());
            query = repository.find(queryStr.toString(), sort, params);
        }

        long totalCount = query.count();
        java.util.List<TbPenjualanEntity> rows = query.page(io.quarkus.panache.common.Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage())).list();

        return new com.github.b3kt.application.dto.PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }
}

