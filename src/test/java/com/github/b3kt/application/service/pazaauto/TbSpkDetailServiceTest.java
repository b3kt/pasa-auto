package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailId;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbSpkDetailRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TbSpkDetailServiceTest {

    @Mock
    TbSpkDetailRepository repository;

    @InjectMocks
    com.github.b3kt.application.service.pazaauto.TbSpkDetailService service;

    @Test
    @DisplayName("findByNoSpk delegates to repository")
    void findByNoSpk() {
        TbSpkDetailEntity detail = new TbSpkDetailEntity();
        PanacheQuery<TbSpkDetailEntity> query = mock(PanacheQuery.class);
        when(repository.find(eq("id.noSpk"), eq("SPK001"))).thenReturn(query);
        when(query.list()).thenReturn(List.of(detail));

        List<TbSpkDetailEntity> result = service.findByNoSpk("SPK001");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findByNoSpk returns empty list")
    void findByNoSpkEmpty() {
        PanacheQuery<TbSpkDetailEntity> query = mock(PanacheQuery.class);
        when(repository.find(eq("id.noSpk"), eq("NONE"))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        assertTrue(service.findByNoSpk("NONE").isEmpty());
    }

    @Test
    @DisplayName("setEntityId sets embedded id")
    void setEntityId() {
        TbSpkDetailEntity entity = new TbSpkDetailEntity();
        TbSpkDetailId id = new TbSpkDetailId();
        service.setEntityId(entity, id);
        assertSame(id, entity.getId());
    }

    @Test
    @DisplayName("findAll delegates to repository")
    void testFindAll() {
        TbSpkDetailEntity detail = new TbSpkDetailEntity();
        when(repository.listAll()).thenReturn(List.of(detail));
        assertEquals(1, service.findAll().size());
    }

    @Test
    @DisplayName("findById returns entity when found")
    void testFindById_found() {
        TbSpkDetailId id = new TbSpkDetailId("SPK001", "Jasa A");
        TbSpkDetailEntity detail = new TbSpkDetailEntity();
        detail.setId(id);
        when(repository.findByIdOptional(id)).thenReturn(Optional.of(detail));
        assertNotNull(service.findById(id));
    }

    @Test
    @DisplayName("findById throws when not found")
    void testFindById_notFound() {
        TbSpkDetailId id = new TbSpkDetailId("SPK999", "Missing");
        when(repository.findByIdOptional(id)).thenReturn(Optional.empty());
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.findById(id));
    }

    @Test
    @DisplayName("create persists entity")
    void testCreate() {
        TbSpkDetailEntity detail = new TbSpkDetailEntity();
        doNothing().when(repository).persist(any(TbSpkDetailEntity.class));
        assertNotNull(service.create(detail));
    }
}
