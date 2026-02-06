package com.github.b3kt.application.service;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.persistence.repository.RoleEntityRepository;
import com.github.b3kt.infrastructure.persistence.repository.UserEntityRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import java.util.HashSet;
import java.util.List;

@ApplicationScoped
public class RoleService extends AbstractCrudService<RoleEntity, Long> {

    @Inject
    RoleEntityRepository repository;

    @Inject
    UserEntityRepository userRepository;

    @Override
    protected PanacheRepositoryBase<RoleEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(RoleEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    public PageResponse<RoleEntity> findPaginated(PageRequest pageRequest) {
        PanacheQuery<RoleEntity> query;

        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
            query = repository.find(
                    "lower(name) like ?1 or lower(description) like ?1",
                    searchPattern);
        } else {
            query = repository.findAll();
        }

        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            Sort sort = pageRequest.isDescending()
                    ? Sort.descending(pageRequest.getSortBy())
                    : Sort.ascending(pageRequest.getSortBy());
            query = query.page(Page.of(0, Integer.MAX_VALUE));

            if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
                String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
                query = repository.find(
                        "lower(name) like ?1 or lower(description) like ?1",
                        sort,
                        searchPattern);
            } else {
                query = repository.findAll(sort);
            }
        }

        long totalCount = query.count();
        List<RoleEntity> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage()))
                .list();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }

    /**
     * Get all users assigned to a specific role.
     */
    public List<UserEntity> getUsersByRoleId(Long roleId) {
        // Verify role exists
        RoleEntity role = repository.findByIdOptional(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + roleId));

        // Find all users that have this role in their rbacRoles set
        return userRepository.find("SELECT u FROM UserEntity u JOIN u.roles r WHERE r.id = ?1", roleId)
                .list();
    }

    /**
     * Update the list of users assigned to a role.
     * This replaces all current assignments with the new list.
     */
    @Transactional
    public void updateRoleUsers(Long roleId, List<Long> userIds) {
        // Verify role exists
        RoleEntity role = repository.findByIdOptional(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + roleId));

        // Get all users currently assigned to this role
        List<UserEntity> currentUsers = getUsersByRoleId(roleId);

        // Remove this role from all currently assigned users
        for (UserEntity user : currentUsers) {
            user.getRoles().remove(role);
            userRepository.persist(user);
        }

        // Assign role to new users
        if (userIds != null && !userIds.isEmpty()) {
            for (Long userId : userIds) {
                UserEntity user = userRepository.findByIdOptional(userId)
                        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

                // Add role to user's rbacRoles set
                if (user.getRoles() == null) {
                    user.setRoles(new HashSet<>());
                }
                user.getRoles().add(role);
                userRepository.persist(user);
            }
        }
    }
}
