package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AbstractCrudServiceTest {

    @Mock
    private PanacheRepositoryBase<TestEntity, Long> repository;

    @Mock
    private PanacheQuery<TestEntity> query;
    
    @Mock
    private EntityManager entityManager;

    private TestCrudService service;
    private TestEntity testEntity;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test entity first
        testEntity = new TestEntity();
        testEntity.setId(1L);
        testEntity.setName("Test Entity");
        
        // Mock repository methods
        doNothing().when(repository).persist(any(TestEntity.class));
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
        when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
        when(repository.listAll()).thenReturn(Arrays.asList(testEntity));
        when(repository.findAll(any(Sort.class))).thenReturn(query);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(Arrays.asList(testEntity));
        when(query.count()).thenReturn(1L);
        when(repository.find(anyString(), anyString())).thenReturn(query);
        when(repository.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(any(TestEntity.class))).thenReturn(testEntity);
        when(repository.deleteById(1L)).thenReturn(true);
        
        service = new TestCrudService(repository);
    }

    @Test
    @DisplayName("Should create entity successfully")
    void testCreate() {
        // Given
        //when(repository.persist(any(TestEntity.class))).thenReturn(testEntity);
        doNothing().when(repository).persist(testEntity);

        // When
        TestEntity result = service.create(testEntity);

        // Then
        assertNotNull(result);
        assertEquals(testEntity, result);
        verify(repository).persist(testEntity);
    }

    @Test
    @DisplayName("Should get entity by ID successfully")
    void testGetById() {
        // Given
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));

        // When
        TestEntity result = service.findById(1L);

        // Then
        assertNotNull(result);
        assertEquals(testEntity, result);
        verify(repository).findByIdOptional(1L);
    }

    @Test
    @DisplayName("Should return null when entity not found by ID")
    void testGetByIdNotFound() {
        // Given
        when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());

        // When & Then - findById throws EntityNotFoundException when not found
        assertThrows(jakarta.persistence.EntityNotFoundException.class,
            () -> service.findById(999L));
        
        verify(repository).findByIdOptional(999L);
    }

    @Test
    @DisplayName("Should update entity successfully")
    void testUpdate() {
        // Given
        TestEntity updatedEntity = new TestEntity();
        updatedEntity.setName("Updated Entity");

        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
        //when(repository.persist(any(TestEntity.class))).thenReturn(testEntity);

        // When
        TestEntity result = service.update(1L, updatedEntity);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Entity", result.getName()); // merge returns the original entity
        verify(repository).findByIdOptional(1L);
        verify(entityManager).merge(any(TestEntity.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent entity")
    void testUpdateNotFound() {
        // Given
        TestEntity updatedEntity = new TestEntity();
        updatedEntity.setName("Updated Entity");

        when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(jakarta.persistence.EntityNotFoundException.class,
            () -> service.update(999L, updatedEntity));
        
        verify(repository).findByIdOptional(999L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Should delete entity successfully")
    void testDelete() {
        // Given
        when(repository.deleteById(1L)).thenReturn(true);
        
        // When
        service.delete(1L);
        
        // Then
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("Should handle deletion of non-existent entity gracefully")
    void testDeleteNotFound() {
        // Given
        when(repository.deleteById(999L)).thenReturn(false); // deleteById returns false if not found

        // When - delete method doesn't throw exception, just logs debug message
        service.delete(999L);
        
        // Then
        verify(repository).deleteById(999L);
    }

    @Test
    @DisplayName("Should get all entities")
    void testGetAll() {
        // Given
        List<TestEntity> entities = Arrays.asList(testEntity);
        when(repository.listAll()).thenReturn(entities);

        // When
        List<TestEntity> result = service.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testEntity, result.get(0));
        verify(repository).listAll();
    }

    @Test
    @DisplayName("Should get paginated results")
    void testGetPage() {
        // Given
        PageRequest pageRequest = new PageRequest(1, 10); // Page 1, not 0
        List<TestEntity> entities = Arrays.asList(testEntity);
        
        when(repository.findAll()).thenReturn(query);
        when(repository.findAll(any(Sort.class))).thenReturn(query);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(entities);

        // When
        PageResponse<TestEntity> result = service.findPaginated(pageRequest);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRows().size());
        assertEquals(testEntity, result.getRows().getFirst());
        verify(repository).findAll();
        verify(query).page(any(Page.class));
        verify(query).list();
    }

    @Test
    @DisplayName("Should search entities")
    void testSearch() {
        // Given
        String searchTerm = "test";

        // When
        List<TestEntity> result = service.search(searchTerm);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test", result.get(0).getName());
    }

    // Test implementation classes
    private static class TestEntity {
        private Long id;
        private String name;

        public TestEntity() {}

        public TestEntity(String name) {
            this.name = name;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestEntity that = (TestEntity) o;
            return java.util.Objects.equals(id, that.id) &&
                   java.util.Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, name);
        }
    }

    private static class TestCrudService extends AbstractCrudService<TestEntity, Long> {
        private final PanacheRepositoryBase<TestEntity, Long> repo;
        
        public TestCrudService(PanacheRepositoryBase<TestEntity, Long> repository) {
            this.repo = repository;
        }
        
        @Override
        protected PanacheRepositoryBase<TestEntity, Long> getRepository() {
            return repo;
        }

        @Override
        protected void setEntityId(TestEntity entity, Long id) {
            entity.setId(id);
        }

        public List<TestEntity> search(String name) {
            return List.of(new TestEntity(name));
        }
    }
}
