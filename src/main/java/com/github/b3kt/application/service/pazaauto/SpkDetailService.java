package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbBarangEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbJasaEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailId;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbBarangRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbJasaRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkDetailRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class SpkDetailService {

    private final TbSpkDetailRepository detailRepository;
    private final TbBarangRepository barangRepository;
    private final TbJasaRepository jasaRepository;

    @Inject
    public SpkDetailService(TbSpkDetailRepository detailRepository, TbBarangRepository barangRepository, 
                           TbJasaRepository jasaRepository) {
        this.detailRepository = detailRepository;
        this.barangRepository = barangRepository;
        this.jasaRepository = jasaRepository;
    }

    @Transactional
    public void saveDetails(TbSpkEntity entity) {
        if (entity.getDetails() != null) {
            for (TbSpkDetailEntity detail : entity.getDetails()) {
                if (detail.getId() == null) {
                    detail.setId(new TbSpkDetailId());
                }
                detail.getId().setNoSpk(entity.getNoSpk());

                // Ensure hargaMaster is populated from master data if not set
                if (detail.getHargaMaster() == null) {
                    if (detail.getSparepartId() != null) {
                        barangRepository.findByIdOptional(detail.getSparepartId())
                            .ifPresent(barang -> detail.setHargaMaster(barang.getHargaJual()));
                    } else if (detail.getJasaId() != null) {
                        jasaRepository.findByIdOptional(detail.getJasaId())
                            .ifPresent(jasa -> detail.setHargaMaster(
                                jasa.getHargaJasa() != null ? BigDecimal.valueOf(jasa.getHargaJasa()) : null));
                    }
                }
                // If no custom price set, harga equals hargaMaster
                if (detail.getHarga() == null && detail.getHargaMaster() != null) {
                    detail.setHarga(detail.getHargaMaster());
                }

                detailRepository.persist(detail);
            }
        }
    }

    @Transactional
    public void deleteDetailsByNoSpk(String noSpk) {
        detailRepository.delete("id.noSpk", noSpk);
    }

    public List<TbSpkDetailEntity> findByNoSpk(String noSpk) {
        return detailRepository.find("id.noSpk", noSpk).list();
    }
}