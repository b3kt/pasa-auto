package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganKendaraanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PelangganVehicleResolverTest {

    @Mock
    TbPelangganKendaraanRepository ownershipRepository;

    @Mock
    TbKendaraanRepository kendaraanRepository;

    @InjectMocks
    PelangganVehicleResolver resolver;

    private TbPelangganKendaraanEntity row(Long id, String nopol, Long kendaraanId, LocalDate mulai) {
        TbPelangganKendaraanEntity row = new TbPelangganKendaraanEntity();
        row.setId(id);
        row.setPelangganId(7L);
        row.setNopol(nopol);
        row.setKendaraanId(kendaraanId);
        row.setTanggalMulai(mulai);
        row.setCurrent(true);
        return row;
    }

    private TbKendaraanEntity master(String merk, String jenis) {
        TbKendaraanEntity k = new TbKendaraanEntity();
        k.setMerk(merk);
        k.setJenis(jenis);
        return k;
    }

    private TbPelangganEntity pelanggan(String legacyNopol) {
        TbPelangganEntity p = new TbPelangganEntity();
        p.setId(7L);
        p.setNopol(legacyNopol);
        return p;
    }

    @Test
    @DisplayName("the most recently acquired current vehicle is primary, regardless of the legacy plate")
    void latestAcquiredWins() {
        when(ownershipRepository.findByPelangganIdCurrent(7L)).thenReturn(List.of(
                row(1L, "B2222BB", 10L, LocalDate.of(2020, 1, 1)),
                row(2L, "B3333CC", 20L, LocalDate.of(2024, 1, 1))));
        when(kendaraanRepository.findByIdCached(20L)).thenReturn(Optional.of(master("Honda", "Brio")));

        TbPelangganEntity p = pelanggan("B2222BB");
        resolver.enrichVehicleInfo(p);

        assertEquals("B3333CC", p.getNopol());
        assertEquals("Honda", p.getMerk());
        assertEquals("Brio", p.getJenis());
    }

    @Test
    @DisplayName("ties on the start date go to the highest id (same rule as the DB trigger)")
    void tieGoesToHighestId() {
        LocalDate sameDay = LocalDate.of(2024, 1, 1);
        when(ownershipRepository.findByPelangganIdCurrent(7L)).thenReturn(List.of(
                row(5L, "B5555EE", 10L, sameDay),
                row(9L, "B9999II", 20L, sameDay)));

        TbPelangganEntity p = pelanggan(null);
        resolver.enrichVehicleInfo(p);

        assertEquals("B9999II", p.getNopol());
    }

    @Test
    @DisplayName("a customer without a current vehicle is left untouched")
    void noVehicle() {
        when(ownershipRepository.findByPelangganIdCurrent(7L)).thenReturn(List.of());

        TbPelangganEntity p = pelanggan(null);
        resolver.enrichVehicleInfo(p);

        assertNull(p.getNopol());
        verifyNoInteractions(kendaraanRepository);
    }
}
