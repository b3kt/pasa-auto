package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPembelianRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@ApplicationScoped
public class TbPembelianService extends AbstractCrudService<TbPembelianEntity, Long> {

    @Inject
    TbPembelianRepository repository;

    @Inject
    TbPembelianDetailService detailService;

    @Inject
    TbSparepartService sparepartService;

    @ConfigProperty(name = "app.features.stock-integration.enabled", defaultValue = "false")
    Boolean stockIntegrationEnabled;

    @Override
    protected PanacheRepositoryBase<TbPembelianEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbPembelianEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    @Transactional
    public TbPembelianEntity create(TbPembelianEntity entity) {
        setNoUrutFromNoPembelian(entity);
        // Save main pembelian record
        TbPembelianEntity saved = super.create(entity);
        return saved;
    }

    @Transactional
    public TbPembelianEntity createWithDetails(TbPembelianEntity entity, List<TbPembelianDetailEntity> details) {
        setNoUrutFromNoPembelian(entity);
        // Save main pembelian record
        TbPembelianEntity saved = super.create(entity);
        
        // Save detail records
        if (details != null && !details.isEmpty()) {
            for (TbPembelianDetailEntity detail : details) {
                detail.setPembelianId(saved.getId());
                detailService.create(detail);

                // Update stock if feature is enabled and this is a sparepart purchase
                if (stockIntegrationEnabled && "SPAREPART".equals(entity.getJenisPembelian())) {
                    if (detail.getSparepartId() != null && detail.getKuantiti() != null) {
                        sparepartService.increaseStock(detail.getSparepartId(), detail.getKuantiti());
                    }
                }
            }
        }

        return saved;
    }

    @Override
    @Transactional
    public TbPembelianEntity update(Long id, TbPembelianEntity entity) {
        // For update, we need to handle stock reversal and re-application
        // First, get the old entity to reverse stock changes
        TbPembelianEntity oldEntity = findById(id);
        List<TbPembelianDetailEntity> oldDetails = detailService.findByPembelianId(id);

        // Reverse old stock changes if feature is enabled
        if (stockIntegrationEnabled && "SPAREPART".equals(oldEntity.getJenisPembelian())) {
            for (TbPembelianDetailEntity oldDetail : oldDetails) {
                if (oldDetail.getSparepartId() != null && oldDetail.getKuantiti() != null) {
                    sparepartService.decreaseStock(oldDetail.getSparepartId(), oldDetail.getKuantiti());
                }
            }
        }

        // Update main record
        TbPembelianEntity updated = super.update(id, entity);

        return updated;
    }

    @Transactional
    public TbPembelianEntity updateWithDetails(Long id, TbPembelianEntity entity,
            List<TbPembelianDetailEntity> details) {
        // Update main entity (this will handle stock reversal)
        TbPembelianEntity updated = update(id, entity);

        // Delete old details
        detailService.deleteByPembelianId(id);

        // Save new details
        if (details != null && !details.isEmpty()) {
            for (TbPembelianDetailEntity detail : details) {
                detail.setId(null); // Ensure it's treated as a new entity
                detail.setPembelianId(updated.getId());
                detailService.create(detail);

                // Apply new stock changes if feature is enabled
                if (stockIntegrationEnabled && "SPAREPART".equals(entity.getJenisPembelian())) {
                    if (detail.getSparepartId() != null && detail.getKuantiti() != null) {
                        sparepartService.increaseStock(detail.getSparepartId(), detail.getKuantiti());
                    }
                }
            }
        }

        return updated;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // Get entity and details before deletion
        TbPembelianEntity entity = findById(id);
        List<TbPembelianDetailEntity> details = detailService.findByPembelianId(id);

        // Reverse stock changes if feature is enabled
        if (stockIntegrationEnabled && "SPAREPART".equals(entity.getJenisPembelian())) {
            for (TbPembelianDetailEntity detail : details) {
                if (detail.getSparepartId() != null && detail.getKuantiti() != null) {
                    sparepartService.decreaseStock(detail.getSparepartId(), detail.getKuantiti());
                }
            }
        }

        // Delete details (will cascade, but explicit for clarity)
        detailService.deleteByPembelianId(id);

        // Delete main record
        super.delete(id);
    }

    @Override
    public PageResponse<TbPembelianEntity> findPaginated(PageRequest pageRequest) {
        // Build query with filters
        StringBuilder queryStr = new StringBuilder("1=1");
        io.quarkus.panache.common.Parameters params = new io.quarkus.panache.common.Parameters();

        // Search filter
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            queryStr.append(" and lower(noPembelian) like :search");
            params.and("search", "%" + pageRequest.getSearch().toLowerCase() + "%");
        }

        // Jenis pembelian filter
        String jenisPembelianFilter = pageRequest.getJenisPembelianFilter();
        if (jenisPembelianFilter != null && !jenisPembelianFilter.isEmpty()) {
            queryStr.append(" and jenisPembelian = :jenisPembelian");
            params.and("jenisPembelian", jenisPembelianFilter);
        }

        // Kategori operasional filter
        String kategoriOperasionalFilter = pageRequest.getKategoriOperasionalFilter();
        if (kategoriOperasionalFilter != null && !kategoriOperasionalFilter.isEmpty()) {
            queryStr.append(" and kategoriOperasional = :kategoriOperasional");
            params.and("kategoriOperasional", kategoriOperasionalFilter);
        }

        // Status pembayaran filter
        if (pageRequest.getStatusFilter() != null && !pageRequest.getStatusFilter().isEmpty()) {
            String[] statuses = pageRequest.getStatusFilter().split(",");
            queryStr.append(" and statusPembayaran in (:statuses)");
            params.and("statuses", java.util.Arrays.asList(statuses));
        }

        // Date range filter
        if (pageRequest.getStartDate() != null && !pageRequest.getStartDate().isEmpty()) {
            LocalDateTime startDateTime = LocalDate.parse(pageRequest.getStartDate()).atStartOfDay();
            queryStr.append(" and tanggalPembelian >= :startDate");
            params.and("startDate", startDateTime);
        }
        if (pageRequest.getEndDate() != null && !pageRequest.getEndDate().isEmpty()) {
            LocalDateTime endDateTime = LocalDate.parse(pageRequest.getEndDate()).atTime(LocalTime.MAX);
            queryStr.append(" and tanggalPembelian <= :endDate");
            params.and("endDate", endDateTime);
        }

        PanacheQuery<TbPembelianEntity> query = repository.find(queryStr.toString(), params);

        // Apply sorting
        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            Sort sort = pageRequest.isDescending()
                    ? Sort.descending(pageRequest.getSortBy())
                    : Sort.ascending(pageRequest.getSortBy());
            query = repository.find(queryStr.toString(), sort, params);
        }

        long totalCount = query.count();
        List<TbPembelianEntity> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage()))
                .list();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }
    public String generateNoPembelian(String jenisPembelian) {
        String code;
        if ("SPAREPART".equalsIgnoreCase(jenisPembelian)) {
            code = "FS";
        } else if ("BARANG".equalsIgnoreCase(jenisPembelian)) {
            code = "FB";
        } else {
            code = "FO";
        }
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().plusDays(1).atStartOfDay();
        
        Integer maxNoUrut = repository.findMaxNoUrut(startOfDay, endOfDay, jenisPembelian);
        int nextNoUrut = (maxNoUrut == null ? 0 : maxNoUrut) + 1;
        
        String noPembelian = String.format("%s-%s-%d", code, datePart, nextNoUrut);
        
        // Ensure uniqueness (collision check)
        while (repository.find("noPembelian", noPembelian).count() > 0) {
            nextNoUrut++;
            noPembelian = String.format("%s-%s-%d", code, datePart, nextNoUrut);
        }
        
        return noPembelian;
    }

    private void setNoUrutFromNoPembelian(TbPembelianEntity entity) {
        if (entity.getNoPembelian() != null && !entity.getNoPembelian().isEmpty()) {
            try {
                String[] parts = entity.getNoPembelian().split("-");
                if (parts.length >= 3) {
                    entity.setNoUrut(Integer.parseInt(parts[2]));
                }
            } catch (NumberFormatException e) {
                // Ignore if format is invalid, noUrut will be null
            }
        }
    }
}
