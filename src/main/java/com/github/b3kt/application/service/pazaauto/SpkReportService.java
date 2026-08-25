package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.RekapPenjualanDto;
import com.github.b3kt.application.helper.QueryFilterBuilder;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkDetailRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SpkReportService {

    private final TbSpkRepository repository;
    private final TbSpkDetailRepository detailRepository;
    private final EntityManager entityManager;
    private final SpkEnrichmentService enrichmentService;

    @Inject
    public SpkReportService(TbSpkRepository repository, TbSpkDetailRepository detailRepository, 
                           EntityManager entityManager, SpkEnrichmentService enrichmentService) {
        this.repository = repository;
        this.detailRepository = detailRepository;
        this.entityManager = entityManager;
        this.enrichmentService = enrichmentService;
    }

    public PageResponse<TbSpkEntity> findPaginated(PageRequest pageRequest) {
        QueryFilterBuilder filterBuilder = QueryFilterBuilder.create()
                .withSearch(pageRequest.getSearch())
                .withStatusFilter(pageRequest.getStatusFilter(), "statusSpk")
                .withDateRange(pageRequest.getStartDate(), pageRequest.getEndDate(), "tanggalJamSpk");

        String queryString = filterBuilder.getQueryString();
        Object[] params = filterBuilder.getParams();

        PanacheQuery<TbSpkEntity> query;
        if (params.length > 0) {
            query = repository.find(queryString, params);
        } else {
            query = repository.find(queryString);
        }

        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            Sort sort = pageRequest.isDescending()
                    ? Sort.descending(pageRequest.getSortBy())
                    : Sort.ascending(pageRequest.getSortBy());
            query = repository.find(queryString, sort, params);
        }

        long totalCount = query.count();
        List<TbSpkEntity> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage())).list();
        enrichmentService.fillRequiredFields(rows);

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }

    public PageResponse<RekapPenjualanDto> findPaginatedWithPenjualan(PageRequest pageRequest) {
        String baseQuery = "SELECT new com.github.b3kt.application.dto.pazaauto.RekapPenjualanDto(s, p) " +
                " FROM com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity s " +
                " LEFT JOIN com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity p " +
                "   ON s.noSpk = p.noSpk " +
                " WHERE 1=1";
        String countQuery = "SELECT COUNT(s) FROM com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity s " +
                " LEFT JOIN com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity p " +
                "   ON s.noSpk = p.noSpk WHERE 1=1";

        QueryFilterBuilder filterBuilder = QueryFilterBuilder.create()
                .withSearch(pageRequest.getSearch())
                .withStatusFilter(pageRequest.getStatusFilter(), "statusSpk")
                .withDateRange(pageRequest.getStartDate(), pageRequest.getEndDate(), "tanggalJamSpk");

        String filterClause = filterBuilder.getQueryString().replace("1=1", "");
        baseQuery += filterClause;
        countQuery += filterClause;

        Object[] params = filterBuilder.getParams();
        TypedQuery<RekapPenjualanDto> query = entityManager.createQuery(baseQuery, RekapPenjualanDto.class);
        Query countQ = entityManager.createQuery(countQuery);

        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
            countQ.setParameter(i + 1, params[i]);
        }

        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            String sortDirection = pageRequest.isDescending() ? "desc" : "asc";
            String sortField = pageRequest.getSortBy();
            
            // Handle special case for grandTotal which comes from TbPenjualanEntity
            if ("grandTotal".equals(sortField)) {
                baseQuery += " order by p." + sortField + " " + sortDirection;
            } else {
                baseQuery += " order by s." + sortField + " " + sortDirection;
            }
            
            query = entityManager.createQuery(baseQuery, RekapPenjualanDto.class);
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i + 1, params[i]);
            }
        }

        long totalCount = (Long) countQ.getSingleResult();
        int firstResult = (pageRequest.getPage() - 1) * pageRequest.getRowsPerPage();
        query.setFirstResult(firstResult);
        query.setMaxResults(pageRequest.getRowsPerPage());
        List<RekapPenjualanDto> rows = query.getResultList();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }

    public RekapPenjualanDto findByIdWithPenjualan(Long id) {
        String queryString = "SELECT new com.github.b3kt.application.dto.pazaauto.RekapPenjualanDto(s, p) " +
                " FROM com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity s " +
                " LEFT JOIN com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity p " +
                "   ON s.noSpk = p.noSpk " +
                " WHERE s.id = ?1 ";
        Query query = entityManager.createQuery(queryString)
                .setParameter(1, id);
        RekapPenjualanDto entity = (RekapPenjualanDto) query.getSingleResult();
        if (entity != null) {
            List<com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity> details = detailRepository
                    .find("id.noSpk", entity.getNoSpk()).list();
            entity.setDetails(details);
            enrichmentService.enrich(entity);
        }
        return entity;
    }

    public List<TbSpkEntity> findUnprocessedSpk() {
        List<TbSpkEntity> list = repository.find("statusSpk in ('PROSES', 'SELESAI') and noSpk not in (select noSpk from TbPenjualanEntity where noSpk is not null)").list();
        enrichmentService.fillRequiredFields(list);
        return list;
    }

    public TbSpkEntity findByNoSpk(String noSpk) {
        TbSpkEntity entity = repository.find("noSpk", noSpk).firstResult();
        if (entity != null) {
            enrichmentService.fillRequiredFields(List.of(entity));
        }
        return entity;
    }
}