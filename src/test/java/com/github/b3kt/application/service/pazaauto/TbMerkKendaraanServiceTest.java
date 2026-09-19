package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbMerkKendaraanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKendaraanRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbMerkKendaraanRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TbMerkKendaraanService Tests")
class TbMerkKendaraanServiceTest {

    @Mock
    TbMerkKendaraanRepository repository;

    @Mock
    TbKendaraanRepository kendaraanRepository;

    @InjectMocks
    TbMerkKendaraanService service;

    private TbMerkKendaraanEntity honda;

    @BeforeEach
    void setUp() {
        honda = new TbMerkKendaraanEntity();
        honda.setId(1L);
        honda.setNama("HONDA");
    }

    @Test
    @DisplayName("create normalizes nama to trimmed uppercase")
    void testCreate_normalizes() {
        when(repository.findByNamaIgnoreCase("TOYOTA")).thenReturn(Optional.empty());
        TbMerkKendaraanEntity input = new TbMerkKendaraanEntity();
        input.setNama("  toyota ");

        TbMerkKendaraanEntity result = service.create(input);

        assertEquals("TOYOTA", result.getNama());
        verify(repository).persist(input);
    }

    @Test
    @DisplayName("create rejects duplicate nama")
    void testCreate_duplicate() {
        when(repository.findByNamaIgnoreCase("HONDA")).thenReturn(Optional.of(honda));
        TbMerkKendaraanEntity input = new TbMerkKendaraanEntity();
        input.setNama("honda");

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verify(repository, never()).persist(any(TbMerkKendaraanEntity.class));
    }

    @Test
    @DisplayName("create rejects blank nama")
    void testCreate_blank() {
        TbMerkKendaraanEntity input = new TbMerkKendaraanEntity();
        input.setNama("   ");

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
    }

    @Test
    @DisplayName("update keeps own nama and syncs deprecated tb_kendaraan.merk")
    void testUpdate_syncsKendaraanMerk() {
        when(repository.findByNamaIgnoreCase("HONDA MOTOR")).thenReturn(Optional.empty());
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(honda));
        EntityManager em = mock(EntityManager.class);
        when(repository.getEntityManager()).thenReturn(em);
        when(em.merge(any(TbMerkKendaraanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        TbMerkKendaraanEntity input = new TbMerkKendaraanEntity();
        input.setNama("honda motor");

        TbMerkKendaraanEntity result = service.update(1L, input);

        assertEquals("HONDA MOTOR", result.getNama());
        verify(kendaraanRepository).update("merk = ?1 where merkId = ?2", "HONDA MOTOR", 1L);
    }

    @Test
    @DisplayName("update allows saving the same nama on itself")
    void testUpdate_sameNama() {
        when(repository.findByNamaIgnoreCase("HONDA")).thenReturn(Optional.of(honda));
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(honda));
        EntityManager em = mock(EntityManager.class);
        when(repository.getEntityManager()).thenReturn(em);
        when(em.merge(any(TbMerkKendaraanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        TbMerkKendaraanEntity input = new TbMerkKendaraanEntity();
        input.setNama("Honda");
        input.setKeterangan("Jepang");

        assertDoesNotThrow(() -> service.update(1L, input));
    }

    @Test
    @DisplayName("delete is blocked while kendaraan still reference the merk")
    void testDelete_blocked() {
        when(kendaraanRepository.count("merkId", 1L)).thenReturn(3L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.delete(1L));
        assertTrue(ex.getMessage().contains("Cannot delete merk"));
        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("delete succeeds when merk is unused")
    void testDelete_ok() {
        when(kendaraanRepository.count("merkId", 1L)).thenReturn(0L);
        when(repository.deleteById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }
}
