package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SpkNumberService {

    private final TbSpkRepository repository;

    @Inject
    public SpkNumberService(TbSpkRepository repository) {
        this.repository = repository;
    }

    public String getNextSpkNumber(String spkNumber) {
        final String spkPattern = spkNumber + "%";
        return repository
                .find("where noSpk LIKE :spkPattern order by id desc", io.quarkus.panache.common.Parameters.with("spkPattern", spkPattern))
                .list()
                .stream()
                .map(TbSpkEntity::getNoSpk)
                .findFirst()
                .orElse(spkNumber + "00");
    }

    public String generateNextSpkNumber(String datePrefix) {
        String lastSpkNumber = getNextSpkNumber(datePrefix);
        String lastQueueNumber = lastSpkNumber.substring(lastSpkNumber.length() - 2);
        int nextQueueNumber = Integer.parseInt(lastQueueNumber) + 1;
        return lastSpkNumber.substring(0, lastSpkNumber.length() - 2)
                + String.format("%02d", nextQueueNumber);
    }
}