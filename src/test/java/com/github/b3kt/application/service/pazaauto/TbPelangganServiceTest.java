package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.domain.model.pazaauto.Pelanggan;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.repository.PelangganRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TbPelangganService Tests")
class TbPelangganServiceTest {

    @Mock
    private PelangganRepository pelangganRepository;

    @InjectMocks
    private TbPelangganService pelangganService;

    private Pelanggan testDomain;
    private TbPelangganEntity testEntity;

    @BeforeEach
    void setUp() {
        testDomain = new Pelanggan("B1234CD", "John Doe", "Toyota");
        testDomain.setId(1L);

        testEntity = new TbPelangganEntity();
        testEntity.setId(1L);
        testEntity.setNopol("B1234CD");
        testEntity.setNamaPelanggan("John Doe");
        testEntity.setMerk("Toyota");
    }

    @Test
    @DisplayName("Should find all pelanggan")
    void testFindAll() {
        when(pelangganRepository.findAll()).thenReturn(List.of(testDomain));

        List<TbPelangganEntity> result = pelangganService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("B1234CD", result.get(0).getNopol());
        assertEquals("John Doe", result.get(0).getNamaPelanggan());
        verify(pelangganRepository).findAll();
    }

    @Test
    @DisplayName("Should find pelanggan by ID when found")
    void testFindById_found() {
        when(pelangganRepository.findById(1L)).thenReturn(Optional.of(testDomain));

        TbPelangganEntity result = pelangganService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("B1234CD", result.getNopol());
        verify(pelangganRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return null when pelanggan not found by ID")
    void testFindById_notFound() {
        when(pelangganRepository.findById(999L)).thenReturn(Optional.empty());

        TbPelangganEntity result = pelangganService.findById(999L);

        assertNull(result);
        verify(pelangganRepository).findById(999L);
    }

    @Test
    @DisplayName("Should find paginated pelanggan")
    void testFindPaginated() {
        PageRequest pageRequest = new PageRequest(1, 10);
        PageResponse<Pelanggan> domainPage = new PageResponse<>(List.of(testDomain), 1, 10, 1);
        when(pelangganRepository.findPaginated(pageRequest)).thenReturn(domainPage);

        PageResponse<TbPelangganEntity> result = pelangganService.findPaginated(pageRequest);

        assertNotNull(result);
        assertEquals(1, result.getRows().size());
        assertEquals("B1234CD", result.getRows().get(0).getNopol());
        assertEquals(1L, result.getRowsNumber());
        verify(pelangganRepository).findPaginated(pageRequest);
    }

    @Test
    @DisplayName("Should create pelanggan")
    void testCreate() {
        when(pelangganRepository.save(any(Pelanggan.class))).thenReturn(testDomain);

        TbPelangganEntity result = pelangganService.create(testEntity);

        assertNotNull(result);
        assertEquals("B1234CD", result.getNopol());
        verify(pelangganRepository).save(any(Pelanggan.class));
    }

    @Test
    @DisplayName("Should update pelanggan when found")
    void testUpdate_found() {
        when(pelangganRepository.findById(1L)).thenReturn(Optional.of(testDomain));
        when(pelangganRepository.save(any(Pelanggan.class))).thenReturn(testDomain);

        TbPelangganEntity inputEntity = new TbPelangganEntity();
        inputEntity.setNopol("B5678EF");
        inputEntity.setNamaPelanggan("Jane Doe");
        inputEntity.setMerk("Honda");

        TbPelangganEntity result = pelangganService.update(1L, inputEntity);

        assertNotNull(result);
        verify(pelangganRepository).findById(1L);
        verify(pelangganRepository).save(any(Pelanggan.class));
    }

    @Test
    @DisplayName("Should return null when updating non-existent pelanggan")
    void testUpdate_notFound() {
        when(pelangganRepository.findById(999L)).thenReturn(Optional.empty());

        TbPelangganEntity result = pelangganService.update(999L, testEntity);

        assertNull(result);
        verify(pelangganRepository).findById(999L);
        verify(pelangganRepository, never()).save(any(Pelanggan.class));
    }

    @Test
    @DisplayName("Should delete pelanggan by ID")
    void testDelete() {
        doNothing().when(pelangganRepository).deleteById(1L);

        pelangganService.delete(1L);

        verify(pelangganRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should find pelanggan by nopol when found")
    void testFindByNopol_found() {
        when(pelangganRepository.findByNopol("B1234CD")).thenReturn(Optional.of(testDomain));

        TbPelangganEntity result = pelangganService.findByNopol("B1234CD");

        assertNotNull(result);
        assertEquals("B1234CD", result.getNopol());
        assertEquals("John Doe", result.getNamaPelanggan());
        verify(pelangganRepository).findByNopol("B1234CD");
    }

    @Test
    @DisplayName("Should return null when pelanggan not found by nopol")
    void testFindByNopol_notFound() {
        when(pelangganRepository.findByNopol("NONEXISTENT")).thenReturn(Optional.empty());

        TbPelangganEntity result = pelangganService.findByNopol("NONEXISTENT");

        assertNull(result);
        verify(pelangganRepository).findByNopol("NONEXISTENT");
    }

    @Test
    @DisplayName("Should patch pelanggan by nopol when found")
    void testPatchByNopol_found() {
        when(pelangganRepository.findByNopol("B1234CD")).thenReturn(Optional.of(testDomain));
        when(pelangganRepository.save(any(Pelanggan.class))).thenReturn(testDomain);

        TbPelangganEntity patchData = new TbPelangganEntity();
        patchData.setNamaPelanggan("Updated Name");

        TbPelangganEntity result = pelangganService.patchByNopol("B1234CD", patchData);

        assertNotNull(result);
        verify(pelangganRepository).findByNopol("B1234CD");
        verify(pelangganRepository).save(any(Pelanggan.class));
    }

    @Test
    @DisplayName("Should return null when patching non-existent pelanggan by nopol")
    void testPatchByNopol_notFound() {
        when(pelangganRepository.findByNopol("NONEXISTENT")).thenReturn(Optional.empty());

        TbPelangganEntity result = pelangganService.patchByNopol("NONEXISTENT", testEntity);

        assertNull(result);
        verify(pelangganRepository).findByNopol("NONEXISTENT");
        verify(pelangganRepository, never()).save(any(Pelanggan.class));
    }

    @Test
    @DisplayName("Should patch all fields by nopol")
    void testPatchByNopol_allFields() {
        when(pelangganRepository.findByNopol("B1234CD")).thenReturn(Optional.of(testDomain));
        when(pelangganRepository.save(any(Pelanggan.class))).thenReturn(testDomain);

        TbPelangganEntity patchData = new TbPelangganEntity();
        patchData.setNamaPelanggan("Updated Name");
        patchData.setAlamat("Jl. Test");
        patchData.setMerk("Honda");
        patchData.setJenis("SUV");
        patchData.setContactPerson("Contact");
        patchData.setTelepon("021-1234");
        patchData.setEmail("test@test.com");
        patchData.setJenisKelamin("Laki-laki");
        patchData.setKeterangan("Keterangan");
        patchData.setKodePos("12345");
        patchData.setKota("Jakarta");
        patchData.setNoHp("08123456789");
        patchData.setNoTelepon("021-9876");

        TbPelangganEntity result = pelangganService.patchByNopol("B1234CD", patchData);

        assertNotNull(result);
        verify(pelangganRepository).save(any(Pelanggan.class));
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException from getRepository")
    void testGetRepository_throwsUnsupported() {
        assertThrows(UnsupportedOperationException.class, () -> pelangganService.getRepository());
    }

    @Test
    @DisplayName("Should set entity id via setEntityId")
    void testSetEntityId() {
        TbPelangganEntity entity = new TbPelangganEntity();
        pelangganService.setEntityId(entity, 42L);
        assertEquals(42L, entity.getId());
    }
}
