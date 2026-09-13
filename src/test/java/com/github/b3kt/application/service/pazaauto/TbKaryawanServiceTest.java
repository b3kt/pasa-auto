package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanPosisiEntity;
import com.github.b3kt.infrastructure.persistence.repository.RoleEntityRepository;
import com.github.b3kt.infrastructure.persistence.repository.UserEntityRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanPosisiRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TbKaryawanServiceTest {

    @Mock
    TbKaryawanRepository repository;

    @Mock
    TbKaryawanPosisiRepository karyawanPosisiRepository;

    @Mock
    UserEntityRepository userRepository;

    @Mock
    RoleEntityRepository roleRepository;

    @Mock
    PanacheQuery<TbKaryawanEntity> query;

    @InjectMocks
    TbKaryawanService service;

    @Mock
    EntityManager entityManager;

    private TbKaryawanEntity entity;

    @BeforeEach
    void setUp() {
        entity = new TbKaryawanEntity();
        entity.setId(1L);
        entity.setNamaKaryawan("John Doe");
        entity.setEmail("john@example.com");
        entity.setIdPosisi(10L);
    }

    // ==================== create() ====================

    @Test
    @DisplayName("create with email uses email as username")
    void create_withEmail_usesEmailAsUsername() {
        doAnswer(inv -> {
            inv.getArgument(0, TbKaryawanEntity.class).setId(1L);
            return null;
        }).when(repository).persist(any(TbKaryawanEntity.class));

        when(userRepository.findByUsername("john@example.com")).thenReturn(Optional.empty());
        doNothing().when(userRepository).persist(any(UserEntity.class));

        service.create(entity);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).persist(captor.capture());
        UserEntity savedUser = captor.getValue();
        assertEquals("john@example.com", savedUser.getUsername());
        assertEquals("john@example.com", savedUser.getEmail());
        assertEquals(1L, savedUser.getKaryawanId());
        assertTrue(savedUser.isActive());
    }

    @Test
    @DisplayName("create without email generates username from namaKaryawan and id")
    void create_withoutEmail_generatesUsername() {
        entity.setEmail(null);

        doAnswer(inv -> {
            inv.getArgument(0, TbKaryawanEntity.class).setId(5L);
            return null;
        }).when(repository).persist(any(TbKaryawanEntity.class));

        when(userRepository.findByUsername("johndoe5")).thenReturn(Optional.empty());
        doNothing().when(userRepository).persist(any(UserEntity.class));

        service.create(entity);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).persist(captor.capture());
        assertEquals("johndoe5", captor.getValue().getUsername());
        assertEquals("johndoe5@example.com", captor.getValue().getEmail());
    }

    @Test
    @DisplayName("create without email and with empty email generates username from namaKaryawan and id")
    void create_withEmptyEmail_generatesUsername() {
        entity.setEmail("");

        doAnswer(inv -> {
            inv.getArgument(0, TbKaryawanEntity.class).setId(3L);
            return null;
        }).when(repository).persist(any(TbKaryawanEntity.class));

        when(userRepository.findByUsername("johndoe3")).thenReturn(Optional.empty());
        doNothing().when(userRepository).persist(any(UserEntity.class));

        service.create(entity);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).persist(captor.capture());
        assertEquals("johndoe3", captor.getValue().getUsername());
    }

    @Test
    @DisplayName("create with duplicate generated username appends timestamp")
    void create_duplicateGeneratedUsername_appendsTimestamp() {
        entity.setEmail(null);

        doAnswer(inv -> {
            inv.getArgument(0, TbKaryawanEntity.class).setId(1L);
            return null;
        }).when(repository).persist(any(TbKaryawanEntity.class));

        when(userRepository.findByUsername("johndoe1")).thenReturn(Optional.of(new UserEntity()));
        doNothing().when(userRepository).persist(any(UserEntity.class));

        service.create(entity);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).persist(captor.capture());
        String username = captor.getValue().getUsername();
        assertTrue(username.startsWith("johndoe1"));
        assertNotEquals("johndoe1", username);
    }

    @Test
    @DisplayName("create with custom roles assigns roles from roleRepository")
    void create_withCustomRoles_assignsRoles() {
        entity.setRoles(List.of("admin", "editor"));

        doAnswer(inv -> {
            inv.getArgument(0, TbKaryawanEntity.class).setId(1L);
            return null;
        }).when(repository).persist(any(TbKaryawanEntity.class));

        when(userRepository.findByUsername("john@example.com")).thenReturn(Optional.empty());

        RoleEntity adminRole = new RoleEntity();
        adminRole.setName("admin");
        RoleEntity editorRole = new RoleEntity();
        editorRole.setName("editor");

        when(roleRepository.findByName("admin")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName("editor")).thenReturn(Optional.of(editorRole));
        doNothing().when(userRepository).persist(any(UserEntity.class));

        service.create(entity);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).persist(captor.capture());
        UserEntity savedUser = captor.getValue();
        assertEquals(2, savedUser.getRoles().size());
        assertTrue(savedUser.getRoles().contains(adminRole));
        assertTrue(savedUser.getRoles().contains(editorRole));
    }

    @Test
    @DisplayName("create with null roles defaults to user role")
    void create_withNullRoles_defaultsToUser() {
        entity.setRoles(null);

        doAnswer(inv -> {
            inv.getArgument(0, TbKaryawanEntity.class).setId(1L);
            return null;
        }).when(repository).persist(any(TbKaryawanEntity.class));

        when(userRepository.findByUsername("john@example.com")).thenReturn(Optional.empty());

        RoleEntity userRole = new RoleEntity();
        userRole.setName("user");
        when(roleRepository.findByName("user")).thenReturn(Optional.of(userRole));
        doNothing().when(userRepository).persist(any(UserEntity.class));

        service.create(entity);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).persist(captor.capture());
        UserEntity savedUser = captor.getValue();
        assertEquals(1, savedUser.getRoles().size());
        assertTrue(savedUser.getRoles().contains(userRole));
    }

    @Test
    @DisplayName("create with empty roles defaults to user role")
    void create_withEmptyRoles_defaultsToUser() {
        entity.setRoles(Collections.emptyList());

        doAnswer(inv -> {
            inv.getArgument(0, TbKaryawanEntity.class).setId(1L);
            return null;
        }).when(repository).persist(any(TbKaryawanEntity.class));

        when(userRepository.findByUsername("john@example.com")).thenReturn(Optional.empty());

        RoleEntity userRole = new RoleEntity();
        userRole.setName("user");
        when(roleRepository.findByName("user")).thenReturn(Optional.of(userRole));
        doNothing().when(userRepository).persist(any(UserEntity.class));

        service.create(entity);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).persist(captor.capture());
        assertEquals(1, captor.getValue().getRoles().size());
    }

    @Test
    @DisplayName("create with role not found in repository results in empty roles on user")
    void create_withNonexistentRole_assignsEmptyRoles() {
        entity.setRoles(List.of("nonexistent"));

        doAnswer(inv -> {
            inv.getArgument(0, TbKaryawanEntity.class).setId(1L);
            return null;
        }).when(repository).persist(any(TbKaryawanEntity.class));

        when(userRepository.findByUsername("john@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("nonexistent")).thenReturn(Optional.empty());
        doNothing().when(userRepository).persist(any(UserEntity.class));

        service.create(entity);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).persist(captor.capture());
        assertTrue(captor.getValue().getRoles().isEmpty());
    }

    @Test
    @DisplayName("create persists karyawan entity first")
    void create_persistsKaryawanEntity() {
        doAnswer(inv -> {
            inv.getArgument(0, TbKaryawanEntity.class).setId(1L);
            return null;
        }).when(repository).persist(any(TbKaryawanEntity.class));

        when(userRepository.findByUsername("john@example.com")).thenReturn(Optional.empty());
        doNothing().when(userRepository).persist(any(UserEntity.class));

        TbKaryawanEntity result = service.create(entity);

        verify(repository).persist(entity);
        assertNotNull(result);
        assertEquals(entity, result);
    }

    @Test
    @DisplayName("create sets default password on user")
    void create_setsDefaultPassword() {
        doAnswer(inv -> {
            inv.getArgument(0, TbKaryawanEntity.class).setId(1L);
            return null;
        }).when(repository).persist(any(TbKaryawanEntity.class));

        when(userRepository.findByUsername("john@example.com")).thenReturn(Optional.empty());
        doNothing().when(userRepository).persist(any(UserEntity.class));

        service.create(entity);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).persist(captor.capture());
        assertEquals("password", captor.getValue().getPasswordHash());
    }

    // ==================== setRelationships() ====================

    @Test
    @DisplayName("setRelationships resolves namePosisi from idPosisi")
    void setRelationships_withIdPosisi_resolvesNamePosisi() {
        TbKaryawanPosisiEntity posisi = new TbKaryawanPosisiEntity();
        posisi.setPosisi("Manager");
        when(karyawanPosisiRepository.findByIdOptional(10L)).thenReturn(Optional.of(posisi));

        service.setRelationships(entity);

        assertEquals("Manager", entity.getNamePosisi());
        verify(karyawanPosisiRepository).findByIdOptional(10L);
    }

    @Test
    @DisplayName("setRelationships with null idPosisi does nothing")
    void setRelationships_withNullIdPosisi_doesNothing() {
        entity.setIdPosisi(null);

        service.setRelationships(entity);

        assertNull(entity.getNamePosisi());
        verifyNoInteractions(karyawanPosisiRepository);
    }

    @Test
    @DisplayName("setRelationships with posisi not found keeps namePosisi null")
    void setRelationships_posisiNotFound_keepsNull() {
        when(karyawanPosisiRepository.findByIdOptional(10L)).thenReturn(Optional.empty());

        service.setRelationships(entity);

        assertNull(entity.getNamePosisi());
    }

    // ==================== findById() ====================

    @Test
    @DisplayName("findById returns entity when found")
    void findById_found_returnsEntity() {
        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(entity));

        TbKaryawanEntity result = service.findById(1L);

        assertNotNull(result);
        assertEquals(entity, result);
        verify(repository).findByIdOptional(1L);
    }

    @Test
    @DisplayName("findById throws EntityNotFoundException when not found")
    void findById_notFound_throwsException() {
        when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.findById(999L));
        verify(repository).findByIdOptional(999L);
    }

    // ==================== update() ====================

    @Test
    @DisplayName("update merges and returns entity when found")
    void update_found_mergesEntity() {
        TbKaryawanEntity updatedEntity = new TbKaryawanEntity();
        updatedEntity.setNamaKaryawan("Jane Doe");

        when(repository.findByIdOptional(1L)).thenReturn(Optional.of(entity));
        when(repository.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(any(TbKaryawanEntity.class))).thenReturn(updatedEntity);

        TbKaryawanEntity result = service.update(1L, updatedEntity);

        assertNotNull(result);
        assertEquals("Jane Doe", result.getNamaKaryawan());
        verify(repository).findByIdOptional(1L);
        verify(repository).getEntityManager();
        verify(entityManager).merge(updatedEntity);
        assertEquals(1L, updatedEntity.getId());
    }

    @Test
    @DisplayName("update throws EntityNotFoundException when entity not found")
    void update_notFound_throwsException() {
        TbKaryawanEntity updatedEntity = new TbKaryawanEntity();

        when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.update(999L, updatedEntity));
        verify(repository).findByIdOptional(999L);
        verifyNoInteractions(entityManager);
    }

    // ==================== delete() ====================

    @Test
    @DisplayName("delete calls deleteById on repository")
    void delete_callsDeleteById() {
        when(repository.deleteById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("delete handles non-existent entity gracefully")
    void delete_notFound_handledGracefully() {
        when(repository.deleteById(999L)).thenReturn(false);

        assertDoesNotThrow(() -> service.delete(999L));
        verify(repository).deleteById(999L);
    }

    // ==================== findAll() ====================

    @Test
    @DisplayName("findAll delegates to repository listAll")
    void findAll_delegatesToRepository() {
        List<TbKaryawanEntity> entities = List.of(entity);
        when(repository.listAll()).thenReturn(entities);

        List<TbKaryawanEntity> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals(entity, result.getFirst());
        verify(repository).listAll();
    }

    // ==================== findPaginated() ====================

    @Test
    @DisplayName("findPaginated with search uses PageHelper.paginate")
    void findPaginated_withSearch_usesPaginate() {
        PageRequest pageRequest = new PageRequest(1, 10);
        pageRequest.setSearch("john");

        TbKaryawanPosisiEntity posisi = new TbKaryawanPosisiEntity();
        posisi.setPosisi("Developer");
        when(karyawanPosisiRepository.findByIdOptional(10L)).thenReturn(Optional.of(posisi));

        when(repository.find(anyString(), any(Object[].class))).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any())).thenReturn(query);
        when(query.list()).thenReturn(List.of(entity));

        PageResponse<TbKaryawanEntity> result = service.findPaginated(pageRequest);

        assertNotNull(result);
        assertEquals(1, result.getRows().size());
        assertEquals("Developer", result.getRows().getFirst().getNamePosisi());
        verify(repository).find(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("findPaginated without search uses PageHelper.findAll")
    void findPaginated_withoutSearch_usesFindAll() {
        PageRequest pageRequest = new PageRequest(1, 10);

        when(repository.findAll()).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any())).thenReturn(query);
        when(query.list()).thenReturn(List.of(entity));

        PageResponse<TbKaryawanEntity> result = service.findPaginated(pageRequest);

        assertNotNull(result);
        assertEquals(1, result.getRows().size());
        verify(repository).findAll();
        verify(repository, never()).find(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("findPaginated enriches all rows with setRelationships")
    void findPaginated_enrichesRowsWithRelationships() {
        PageRequest pageRequest = new PageRequest(1, 10);

        TbKaryawanEntity entity2 = new TbKaryawanEntity();
        entity2.setId(2L);
        entity2.setIdPosisi(20L);

        when(repository.findAll()).thenReturn(query);
        when(query.count()).thenReturn(2L);
        when(query.page(any())).thenReturn(query);
        when(query.list()).thenReturn(List.of(entity, entity2));

        TbKaryawanPosisiEntity posisi1 = new TbKaryawanPosisiEntity();
        posisi1.setPosisi("Manager");
        TbKaryawanPosisiEntity posisi2 = new TbKaryawanPosisiEntity();
        posisi2.setPosisi("Developer");

        when(karyawanPosisiRepository.findByIdOptional(10L)).thenReturn(Optional.of(posisi1));
        when(karyawanPosisiRepository.findByIdOptional(20L)).thenReturn(Optional.of(posisi2));

        PageResponse<TbKaryawanEntity> result = service.findPaginated(pageRequest);

        assertEquals("Manager", result.getRows().get(0).getNamePosisi());
        assertEquals("Developer", result.getRows().get(1).getNamePosisi());
    }

    @Test
    @DisplayName("findPaginated with search and sort applies sort via PageHelper")
    void findPaginated_withSearchAndSort_appliesSort() {
        PageRequest pageRequest = new PageRequest(1, 10);
        pageRequest.setSearch("john");
        pageRequest.setSortBy("namaKaryawan");
        pageRequest.setDescending(true);

        when(repository.find(anyString(), any(Object[].class))).thenReturn(query);
        when(repository.find(anyString(), any(io.quarkus.panache.common.Sort.class), any(Object[].class)))
                .thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any())).thenReturn(query);
        when(query.list()).thenReturn(List.of(entity));

        PageResponse<TbKaryawanEntity> result = service.findPaginated(pageRequest);

        assertNotNull(result);
        assertEquals(1, result.getRows().size());
        verify(repository).find(anyString(), any(io.quarkus.panache.common.Sort.class), any(Object[].class));
    }

    @Test
    @DisplayName("findPaginated without search and with sort applies sort via PageHelper.findAll")
    void findPaginated_withoutSearchWithSort_appliesSort() {
        PageRequest pageRequest = new PageRequest(1, 10);
        pageRequest.setSortBy("namaKaryawan");
        pageRequest.setDescending(false);

        when(repository.findAll()).thenReturn(query);
        when(repository.findAll(any(io.quarkus.panache.common.Sort.class))).thenReturn(query);
        when(query.count()).thenReturn(1L);
        when(query.page(any())).thenReturn(query);
        when(query.list()).thenReturn(List.of(entity));

        PageResponse<TbKaryawanEntity> result = service.findPaginated(pageRequest);

        assertNotNull(result);
        verify(repository).findAll(any(io.quarkus.panache.common.Sort.class));
    }

    @Test
    @DisplayName("findPaginated returns empty list when no results")
    void findPaginated_emptyResult_returnsEmptyPage() {
        PageRequest pageRequest = new PageRequest(1, 10);

        when(repository.findAll()).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any())).thenReturn(query);
        when(query.list()).thenReturn(Collections.emptyList());

        PageResponse<TbKaryawanEntity> result = service.findPaginated(pageRequest);

        assertNotNull(result);
        assertTrue(result.getRows().isEmpty());
        assertEquals(0, result.getRowsNumber());
    }

    @Test
    @DisplayName("findPaginated delegates to correct PageHelper method based on search")
    void findPaginated_delegatesCorrectly() {
        PageRequest emptySearch = new PageRequest(1, 10);
        emptySearch.setSearch("");

        when(repository.findAll()).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any())).thenReturn(query);
        when(query.list()).thenReturn(Collections.emptyList());

        service.findPaginated(emptySearch);

        verify(repository).findAll();
        verify(repository, never()).find(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("findPaginated with null search uses PageHelper.findAll")
    void findPaginated_nullSearch_usesFindAll() {
        PageRequest pageRequest = new PageRequest(1, 10);
        pageRequest.setSearch(null);

        when(repository.findAll()).thenReturn(query);
        when(query.count()).thenReturn(0L);
        when(query.page(any())).thenReturn(query);
        when(query.list()).thenReturn(Collections.emptyList());

        service.findPaginated(pageRequest);

        verify(repository).findAll();
    }

    // ==================== findAllUnregistered() ====================

    @Test
    @DisplayName("findAllUnregistered delegates to repository.findAllUnregistered")
    void findAllUnregistered_delegatesToRepository() {
        TbKaryawanEntity unregistered = new TbKaryawanEntity();
        unregistered.setId(2L);
        unregistered.setIdPosisi(20L);

        when(repository.findAllUnregistered()).thenReturn(List.of(unregistered));

        TbKaryawanPosisiEntity posisi = new TbKaryawanPosisiEntity();
        posisi.setPosisi("Staff");
        when(karyawanPosisiRepository.findByIdOptional(20L)).thenReturn(Optional.of(posisi));

        List<TbKaryawanEntity> result = service.findAllUnregistered();

        assertEquals(1, result.size());
        assertEquals("Staff", result.getFirst().getNamePosisi());
        verify(repository).findAllUnregistered();
    }

    @Test
    @DisplayName("findAllUnregistered returns empty list when none found")
    void findAllUnregistered_emptyList() {
        when(repository.findAllUnregistered()).thenReturn(Collections.emptyList());

        List<TbKaryawanEntity> result = service.findAllUnregistered();

        assertTrue(result.isEmpty());
        verify(repository).findAllUnregistered();
        verifyNoInteractions(karyawanPosisiRepository);
    }

    @Test
    @DisplayName("findAllUnregistered enriches all returned entities with setRelationships")
    void findAllUnregistered_enrichesAllEntities() {
        TbKaryawanEntity e1 = new TbKaryawanEntity();
        e1.setId(1L);
        e1.setIdPosisi(10L);

        TbKaryawanEntity e2 = new TbKaryawanEntity();
        e2.setId(2L);
        e2.setIdPosisi(null);

        TbKaryawanEntity e3 = new TbKaryawanEntity();
        e3.setId(3L);
        e3.setIdPosisi(30L);

        when(repository.findAllUnregistered()).thenReturn(List.of(e1, e2, e3));

        TbKaryawanPosisiEntity posisi1 = new TbKaryawanPosisiEntity();
        posisi1.setPosisi("Manager");
        TbKaryawanPosisiEntity posisi3 = new TbKaryawanPosisiEntity();
        posisi3.setPosisi("Director");

        when(karyawanPosisiRepository.findByIdOptional(10L)).thenReturn(Optional.of(posisi1));
        when(karyawanPosisiRepository.findByIdOptional(30L)).thenReturn(Optional.of(posisi3));

        List<TbKaryawanEntity> result = service.findAllUnregistered();

        assertEquals(3, result.size());
        assertEquals("Manager", result.get(0).getNamePosisi());
        assertNull(result.get(1).getNamePosisi());
        assertEquals("Director", result.get(2).getNamePosisi());
        verify(karyawanPosisiRepository).findByIdOptional(10L);
        verify(karyawanPosisiRepository).findByIdOptional(30L);
        verify(karyawanPosisiRepository, never()).findByIdOptional(null);
    }

    @Test
    @DisplayName("findAllUnregistered with posisi not found keeps namePosisi null")
    void findAllUnregistered_posisiNotFound_keepsNull() {
        TbKaryawanEntity e1 = new TbKaryawanEntity();
        e1.setId(1L);
        e1.setIdPosisi(99L);

        when(repository.findAllUnregistered()).thenReturn(List.of(e1));
        when(karyawanPosisiRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        List<TbKaryawanEntity> result = service.findAllUnregistered();

        assertNull(result.getFirst().getNamePosisi());
    }

    @Test
    @DisplayName("create with email that is already taken by another user keeps the email as username")
    void create_emailTaken_keepsEmailAsUsername() {
        doAnswer(inv -> {
            inv.getArgument(0, TbKaryawanEntity.class).setId(1L);
            return null;
        }).when(repository).persist(any(TbKaryawanEntity.class));

        when(userRepository.findByUsername("john@example.com")).thenReturn(Optional.of(new UserEntity()));
        doNothing().when(userRepository).persist(any(UserEntity.class));

        service.create(entity);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).persist(captor.capture());
        assertEquals("john@example.com", captor.getValue().getUsername());
    }
}
