package com.github.b3kt.infrastructure.repository;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.domain.model.pazaauto.Pelanggan;

import java.util.List;
import java.util.Optional;

public interface PelangganRepository {

    Optional<Pelanggan> findById(Long id);

    Pelanggan save(Pelanggan pelanggan);

    void deleteById(Long id);

    List<Pelanggan> findAll();

    PageResponse<Pelanggan> findPaginated(PageRequest pageRequest);

    Optional<Pelanggan> findByNopol(String nopol);
}
