package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.dto.pazaauto.RekapPenjualanDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkDetailRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
public class TbSpkService extends AbstractCrudService<TbSpkEntity, Long> {

    private final TbSpkRepository repository;
    private final TbSpkDetailRepository detailRepository;
    private final SpkDetailService spkDetailService;
    private final SpkEnrichmentService enrichmentService;
    private final SpkReportService reportService;
    private final SpkNumberService numberService;

    @Override
    protected TbSpkRepository getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbSpkEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    @Transactional
    public TbSpkEntity create(TbSpkEntity entity) {
        // Persist SPK first to get ID/NoSPK if needed (though NoSPK seems
        if (entity.getNoAntrian() == null) {
            final int noAntrian = Integer.parseInt(entity.getNoSpk().substring(entity.getNoSpk().length() - 2));
            entity.setNoAntrian(noAntrian);
        }

        // pre-generated)
        super.create(entity);

        // Save details
        spkDetailService.saveDetails(entity);

        return entity;
    }

    @Override
    @Transactional
    public TbSpkEntity update(Long id, TbSpkEntity entity) {
        TbSpkEntity updated = super.update(id, entity);

        // Delete existing details
        spkDetailService.deleteDetailsByNoSpk(updated.getNoSpk());

        // update status
        if (entity.isStartProcess()) {
            updated.setStatusSpk("PROSES");
            updated.setStartedAt(LocalDateTime.now());
        }

        // Save new details
        spkDetailService.saveDetails(entity);

        return updated;
    }

    @Override
    public TbSpkEntity findById(Long id) {
        TbSpkEntity entity = super.findById(id);
        if (entity != null) {
            List<TbSpkDetailEntity> details = spkDetailService.findByNoSpk(entity.getNoSpk());
            entity.setDetails(details);
            enrichmentService.enrich(entity);
        }
        return entity;
    }

    public RekapPenjualanDto findByIdWithPenjualan(Long id) {
        return reportService.findByIdWithPenjualan(id);
    }

    @Override
    public PageResponse<TbSpkEntity> findPaginated(PageRequest pageRequest) {
        return reportService.findPaginated(pageRequest);
    }

    public PageResponse<RekapPenjualanDto> findPaginatedWithPenjualan(PageRequest pageRequest) {
        return reportService.findPaginatedWithPenjualan(pageRequest);
    }

    public List<TbSpkEntity> findUnprocessedSpk() {
        return reportService.findUnprocessedSpk();
    }

    public TbSpkEntity findByNoSpk(String noSpk) {
        return reportService.findByNoSpk(noSpk);
    }

    public String getNextSpkNumber(String spkNumber) {
        return numberService.getNextSpkNumber(spkNumber);
    }

    public String generateNextSpkNumber(String datePrefix) {
        return numberService.generateNextSpkNumber(datePrefix);
    }

    @Transactional
    public void deleteByNoSpk(String noSpk) {
        spkDetailService.deleteDetailsByNoSpk(noSpk);
        repository.delete("noSpk", noSpk);
    }

    @Transactional
    public TbSpkEntity cancelSpk(Long id) {
        TbSpkEntity entity = repository.findById(id);
        if (entity != null) {
            String lastStatus = entity.getStatusSpk();
            entity.setKeterangan("SPK Dibatalkan. lastStatus: " + lastStatus);
            entity.setStatusSpk("BATAL");
        }
        return entity;
    }
}
