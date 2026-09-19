package com.github.b3kt.application.service;

import com.github.b3kt.infrastructure.security.PasswordEncoder;
import com.github.b3kt.infrastructure.security.PasswordPolicy;
import jakarta.transaction.Transactional;
import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.service.pazaauto.AbstractCrudService;
import com.github.b3kt.infrastructure.persistence.entity.UserEntity;
import com.github.b3kt.infrastructure.persistence.repository.UserEntityRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class UserService extends AbstractCrudService<UserEntity, Long> {

    @Inject
    UserEntityRepository repository;

    @Override
    protected PanacheRepositoryBase<UserEntity, Long> getRepository() {
        return repository;
    }

    @Inject
    PasswordEncoder passwordEncoder;

    @Inject
    RefreshTokenService refreshTokenService;

    @Override
    protected void setEntityId(UserEntity entity, Long id) {
        entity.setId(id);
    }

    /**
     * The password set here is a temporary one (the Owner knows it), so the user must change it at first login.
     */
    @Override
    @Transactional
    public UserEntity create(UserEntity entity) {
        PasswordPolicy.validate(entity.getPassword(), entity.getUsername());
        entity.setPasswordHash(passwordEncoder.encode(entity.getPassword()));
        entity.setMustChangePassword(true);
        entity.setPassword(null);
        return super.create(entity);
    }

    /**
     * Only profile fields are taken from the request; roles are managed through the RBAC endpoints and the
     * password hash is never accepted from clients. A non-blank {@code password} resets the password
     * (temporary, must be changed) and ends the user's sessions, as does deactivating the user.
     */
    @Override
    @Transactional
    public UserEntity update(Long id, UserEntity entity) {
        UserEntity existing = findById(id);
        String previousUsername = existing.getUsername();

        existing.setUsername(entity.getUsername());
        existing.setEmail(entity.getEmail());
        existing.setKaryawanId(entity.getKaryawanId());
        existing.setActive(entity.isActive());

        boolean endSessions = !entity.isActive();
        if (entity.getPassword() != null && !entity.getPassword().isBlank()) {
            PasswordPolicy.validate(entity.getPassword(), existing.getUsername());
            existing.setPasswordHash(passwordEncoder.encode(entity.getPassword()));
            existing.setMustChangePassword(true);
            endSessions = true;
        }
        if (endSessions) {
            refreshTokenService.revokeAllForUser(previousUsername);
        }
        return existing;
    }

    @Override
    public PageResponse<UserEntity> findPaginated(PageRequest pageRequest) {
        PanacheQuery<UserEntity> query;

        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            String searchPattern = "%" + pageRequest.getSearch().toLowerCase() + "%";
            query = repository.find(
                    "lower(username) like ?1 or lower(email) like ?1",
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
                        "lower(username) like ?1 or lower(email) like ?1",
                        sort,
                        searchPattern);
            } else {
                query = repository.findAll(sort);
            }
        }

        long totalCount = query.count();
        List<UserEntity> rows = query.page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage()))
                .list();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }
}
