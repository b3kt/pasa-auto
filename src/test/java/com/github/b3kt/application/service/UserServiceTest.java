package com.github.b3kt.application.service;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.persistence.repository.UserEntityRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import com.github.b3kt.infrastructure.security.impl.PasswordEncoderImpl;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.github.b3kt.domain.model.ApprovalStatus;
import com.github.b3kt.infrastructure.persistence.entity.AuditTrailEntity;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    UserEntityRepository repository;

    @Mock
    PanacheQuery<UserEntity> query;

    @Mock
    PanacheQuery<UserEntity> sortedQuery;

    @Spy
    PasswordEncoderImpl passwordEncoder = new PasswordEncoderImpl();

    @Mock
    RefreshTokenService refreshTokenService;

    @Mock
    AuditTrailService auditTrailService;

    @InjectMocks
    UserService userService;

    private UserEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new UserEntity();
        testEntity.setId(1L);
        testEntity.setUsername("testuser");
    }

    @Nested
    @DisplayName("AbstractCrudService methods")
    class CrudTests {

        @Test
        @DisplayName("findAll returns all entities")
        void findAll() {
            when(repository.listAll()).thenReturn(List.of(testEntity));
            List<UserEntity> result = userService.findAll();
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("findById returns entity")
        void findById() {
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(testEntity));
            UserEntity result = userService.findById(1L);
            assertNotNull(result);
        }

        @Test
        @DisplayName("findById throws when not found")
        void findByIdNotFound() {
            when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> userService.findById(999L));
        }

        @Test
        @DisplayName("create persists and returns entity")
        void create() {
            doNothing().when(repository).persist(any(UserEntity.class));
            testEntity.setPassword("Temporary-1");

            UserEntity result = userService.create(testEntity);

            assertNotNull(result);
            assertTrue(passwordEncoder.matches("Temporary-1", result.getPasswordHash()));
            assertTrue(result.isMustChangePassword());
            assertNull(result.getPassword());
        }

        @Test
        @DisplayName("create rejects a missing or too short password")
        void createRejectsWeakPassword() {
            assertThrows(IllegalArgumentException.class, () -> userService.create(testEntity));
            testEntity.setPassword("short");
            assertThrows(IllegalArgumentException.class, () -> userService.create(testEntity));
            verify(repository, never()).persist(any(UserEntity.class));
        }

        @Test
        @DisplayName("update copies profile fields and keeps the stored password hash")
        void update() {
            UserEntity existing = new UserEntity();
            existing.setId(1L);
            existing.setUsername("testuser");
            existing.setPasswordHash("$2a$10$existing");
            existing.setActive(true);
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(existing));

            UserEntity request = new UserEntity();
            request.setUsername("testuser");
            request.setEmail("new@example.com");
            request.setActive(true);

            UserEntity result = userService.update(1L, request);

            assertEquals("new@example.com", result.getEmail());
            assertEquals("$2a$10$existing", result.getPasswordHash());
            assertFalse(result.isMustChangePassword());
            verify(refreshTokenService, never()).revokeAllForUser(anyString());
        }

        @Test
        @DisplayName("update with a new password resets it as temporary and ends sessions")
        void updateResetsPassword() {
            UserEntity existing = new UserEntity();
            existing.setId(1L);
            existing.setUsername("testuser");
            existing.setPasswordHash("$2a$10$existing");
            existing.setActive(true);
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(existing));

            UserEntity request = new UserEntity();
            request.setUsername("testuser");
            request.setActive(true);
            request.setPassword("Temporary-2");

            UserEntity result = userService.update(1L, request);

            assertTrue(passwordEncoder.matches("Temporary-2", result.getPasswordHash()));
            assertTrue(result.isMustChangePassword());
            verify(refreshTokenService).revokeAllForUser("testuser");
        }

        @Test
        @DisplayName("deactivating a user ends their sessions")
        void updateDeactivateEndsSessions() {
            UserEntity existing = new UserEntity();
            existing.setId(1L);
            existing.setUsername("testuser");
            existing.setActive(true);
            when(repository.findByIdOptional(1L)).thenReturn(Optional.of(existing));

            UserEntity request = new UserEntity();
            request.setUsername("testuser");
            request.setActive(false);

            userService.update(1L, request);

            verify(refreshTokenService).revokeAllForUser("testuser");
        }

        @Test
        @DisplayName("update throws when not found")
        void updateNotFound() {
            when(repository.findByIdOptional(999L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> userService.update(999L, testEntity));
        }

        @Test
        @DisplayName("delete removes entity")
        void delete() {
            when(repository.deleteById(1L)).thenReturn(true);
            userService.delete(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("delete throws when not found")
        void deleteNotFound() {
            when(repository.deleteById(999L)).thenThrow(new RuntimeException("not found"));
            assertThrows(EntityNotFoundException.class, () -> userService.delete(999L));
        }
    }

    @Nested
    @DisplayName("findPaginated")
    class FindPaginatedTests {

        @Test
        @DisplayName("with search")
        void findPaginatedWithSearch() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSearch("test");
            when(repository.find(anyString(), anyString())).thenReturn(query);
            when(query.count()).thenReturn(1L);
            when(query.page(any(Page.class))).thenReturn(query);
            when(query.list()).thenReturn(List.of(testEntity));

            PageResponse<UserEntity> result = userService.findPaginated(pr);
            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("without search")
        void findPaginatedNoSearch() {
            PageRequest pr = new PageRequest(1, 10);
            when(repository.findAll()).thenReturn(query);
            when(query.count()).thenReturn(1L);
            when(query.page(any(Page.class))).thenReturn(query);
            when(query.list()).thenReturn(List.of(testEntity));

            PageResponse<UserEntity> result = userService.findPaginated(pr);
            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with sort and search")
        void findPaginatedWithSortAndSearch() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSearch("test");
            pr.setSortBy("username");
            when(repository.find(anyString(), anyString())).thenReturn(query);
            when(query.page(any(Page.class))).thenReturn(query);
            when(repository.find(anyString(), any(Sort.class), anyString())).thenReturn(sortedQuery);
            when(sortedQuery.count()).thenReturn(1L);
            when(sortedQuery.page(any(Page.class))).thenReturn(sortedQuery);
            when(sortedQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<UserEntity> result = userService.findPaginated(pr);
            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with sort and no search")
        void findPaginatedWithSortNoSearch() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSortBy("username");
            when(repository.findAll()).thenReturn(query);
            when(query.page(any(Page.class))).thenReturn(query);
            when(repository.findAll(any(Sort.class))).thenReturn(sortedQuery);
            when(sortedQuery.count()).thenReturn(1L);
            when(sortedQuery.page(any(Page.class))).thenReturn(sortedQuery);
            when(sortedQuery.list()).thenReturn(List.of(testEntity));

            PageResponse<UserEntity> result = userService.findPaginated(pr);
            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with sort and search - ascending")
        void findPaginatedWithSortAndSearch_ascending() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSearch("admin");
            pr.setSortBy("username");
            pr.setDescending(false);
            when(repository.find(anyString(), anyString())).thenReturn(query);
            when(query.page(any(Page.class))).thenReturn(query);
            when(repository.find(anyString(), any(Sort.class), anyString())).thenReturn(sortedQuery);
            when(sortedQuery.count()).thenReturn(1L);
            when(sortedQuery.page(any(Page.class))).thenReturn(sortedQuery);
            when(sortedQuery.list()).thenReturn(List.of(new com.github.b3kt.infrastructure.persistence.entity.UserEntity()));

            var result = userService.findPaginated(pr);
            assertEquals(1, result.getRowsNumber());
        }

        @Test
        @DisplayName("with sort and no search - ascending")
        void findPaginatedWithSortNoSearch_ascending() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSortBy("username");
            pr.setDescending(false);
            when(repository.findAll()).thenReturn(query);
            when(query.page(any(Page.class))).thenReturn(query);
            when(repository.findAll(any(Sort.class))).thenReturn(sortedQuery);
            when(sortedQuery.count()).thenReturn(1L);
            when(sortedQuery.page(any(Page.class))).thenReturn(sortedQuery);
            when(sortedQuery.list()).thenReturn(List.of(new com.github.b3kt.infrastructure.persistence.entity.UserEntity()));

            var result = userService.findPaginated(pr);
            assertEquals(1, result.getRowsNumber());
        }
    }

    /**
     * The approval workflow behind Google sign-in: an account the callback created is unusable
     * until an Owner looks at it.
     */
    @Nested
    @DisplayName("pending approval")
    class PendingApproval {

        private UserEntity pending;

        @BeforeEach
        void setUp() {
            pending = new UserEntity();
            pending.setId(7L);
            pending.setUsername("budi");
            pending.setApprovalStatus(ApprovalStatus.PENDING);
            pending.setActive(false);
            when(repository.findByIdOptional(7L)).thenReturn(Optional.of(pending));
        }

        @Test
        @DisplayName("findPending lists only the accounts nobody has looked at")
        void findPendingQueriesByStatus() {
            when(repository.list("approvalStatus", ApprovalStatus.PENDING)).thenReturn(List.of(pending));

            assertEquals(List.of(pending), userService.findPending());
            verify(repository).list("approvalStatus", ApprovalStatus.PENDING);
        }

        /** Approving must not grant access by itself - an Owner assigns roles as a separate act. */
        @Test
        @DisplayName("approve activates the account but grants no roles")
        void approveActivatesWithoutRoles() {
            UserEntity result = userService.approve(7L);

            assertEquals(ApprovalStatus.APPROVED, result.getApprovalStatus());
            assertTrue(result.isActive());
            assertTrue(result.getRoles().isEmpty(), "approving alone must not grant access to anything");
            verify(refreshTokenService, never()).revokeAllForUser(anyString());
        }

        @Test
        @DisplayName("reject deactivates the account and ends any session it holds")
        void rejectRevokesSessions() {
            UserEntity result = userService.reject(7L);

            assertEquals(ApprovalStatus.REJECTED, result.getApprovalStatus());
            assertFalse(result.isActive());
            verify(refreshTokenService).revokeAllForUser("budi");
        }

        /** The users table has no audit trigger - V12 covers only tb_* tables - so this is the only record. */
        @Test
        @DisplayName("both decisions are written to the audit trail")
        void decisionsAreAudited() {
            userService.approve(7L);
            userService.reject(7L);

            ArgumentCaptor<AuditTrailEntity> captor = ArgumentCaptor.forClass(AuditTrailEntity.class);
            verify(auditTrailService, times(2)).record(captor.capture());

            List<AuditTrailEntity> audits = captor.getAllValues();
            assertEquals(List.of("APPROVE", "REJECT"), audits.stream().map(AuditTrailEntity::getAction).toList());
            audits.forEach(audit -> {
                assertEquals("users", audit.getTableName());
                assertEquals(7L, audit.getRecordId());
                assertEquals("budi", audit.getUsername());
            });
        }

        @Test
        @DisplayName("an unknown id is reported rather than silently ignored")
        void unknownIdIsRejected() {
            when(repository.findByIdOptional(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.approve(404L));
            assertThrows(EntityNotFoundException.class, () -> userService.reject(404L));
            verifyNoInteractions(auditTrailService);
        }
    }

    /** The sorted paths re-run the query; a sorted search must keep its filter. */
    @Nested
    @DisplayName("findPaginated sorting")
    class FindPaginatedSorting {

        @Test
        @DisplayName("sorts a filtered search without losing the filter")
        void sortedSearch() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSearch("Budi");
            pr.setSortBy("username");
            pr.setDescending(true);
            when(repository.find(anyString(), any(Object[].class))).thenReturn(query);
            when(query.page(any(Page.class))).thenReturn(query);
            when(repository.find(anyString(), any(Sort.class), any(Object[].class))).thenReturn(sortedQuery);
            when(sortedQuery.count()).thenReturn(2L);
            when(sortedQuery.page(any(Page.class))).thenReturn(sortedQuery);
            when(sortedQuery.list()).thenReturn(List.of(testEntity));

            assertEquals(2L, userService.findPaginated(pr).getRowsNumber());

            ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
            verify(repository).find(anyString(), any(Sort.class), params.capture());
            assertEquals("%budi%", params.getValue()[0], "the search filter survives the sort");
        }

        @Test
        @DisplayName("treats an empty search and sort as unset")
        void emptySearchAndSort() {
            PageRequest pr = new PageRequest(1, 10);
            pr.setSearch("");
            pr.setSortBy("");
            when(repository.findAll()).thenReturn(query);
            when(query.count()).thenReturn(1L);
            when(query.page(any(Page.class))).thenReturn(query);
            when(query.list()).thenReturn(List.of(testEntity));

            assertEquals(1L, userService.findPaginated(pr).getRowsNumber());
            verify(repository).findAll();
            verify(repository, never()).findAll(any(Sort.class));
        }
    }

    /**
     * setEntityId is required by AbstractCrudService, but this class overrides both create and
     * update and neither routes through it - update mutates the managed entity in place. It is
     * exercised directly so the contract still holds if a future change starts relying on it.
     */
    @Test
    @DisplayName("setEntityId stamps the id onto the entity")
    void setEntityIdStampsId() {
        UserEntity entity = new UserEntity();

        userService.setEntityId(entity, 7L);

        assertEquals(7L, entity.getId());
    }

    /** Deactivating a user ends their sessions, as does resetting their password. */
    @Test
    @DisplayName("update ends sessions when the user is deactivated")
    void updateDeactivationEndsSessions() {
        UserEntity existing = new UserEntity();
        existing.setId(7L);
        existing.setUsername("budi");
        existing.setActive(true);
        when(repository.findByIdOptional(7L)).thenReturn(Optional.of(existing));

        UserEntity incoming = new UserEntity();
        incoming.setUsername("budi");
        incoming.setActive(false);

        userService.update(7L, incoming);

        verify(refreshTokenService).revokeAllForUser("budi");
    }

    /** A rename must revoke under the OLD username, or the old sessions survive. */
    @Test
    @DisplayName("update revokes under the previous username when renaming")
    void updateRevokesUnderPreviousUsername() {
        UserEntity existing = new UserEntity();
        existing.setId(7L);
        existing.setUsername("budi");
        existing.setActive(true);
        when(repository.findByIdOptional(7L)).thenReturn(Optional.of(existing));

        UserEntity incoming = new UserEntity();
        incoming.setUsername("budi.baru");
        incoming.setActive(true);
        incoming.setPassword("New-password-1");

        userService.update(7L, incoming);

        assertTrue(existing.isMustChangePassword(), "a reset password must be changed at next login");
        verify(refreshTokenService).revokeAllForUser("budi");
        verify(refreshTokenService, never()).revokeAllForUser("budi.baru");
    }

    /** An active user with no password change keeps their sessions. */
    @Test
    @DisplayName("update keeps sessions when nothing security-relevant changed")
    void updateKeepsSessions() {
        UserEntity existing = new UserEntity();
        existing.setId(7L);
        existing.setUsername("budi");
        existing.setActive(true);
        when(repository.findByIdOptional(7L)).thenReturn(Optional.of(existing));

        UserEntity incoming = new UserEntity();
        incoming.setUsername("budi");
        incoming.setActive(true);
        incoming.setPassword("   ");

        userService.update(7L, incoming);

        verify(refreshTokenService, never()).revokeAllForUser(anyString());
    }
}
