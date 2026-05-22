package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.application.helper.PageHelper;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanPosisiRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbKaryawanRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class TbKaryawanService extends AbstractCrudService<TbKaryawanEntity, Long> {

    @Inject
    TbKaryawanRepository repository;

    @Inject
    TbKaryawanPosisiRepository karyawanPosisiRepository;

    @Inject
    com.github.b3kt.infrastructure.persistence.repository.UserEntityRepository userRepository;

    @Inject
    com.github.b3kt.infrastructure.persistence.repository.RoleEntityRepository roleRepository;

    @Override
    @jakarta.transaction.Transactional
    public TbKaryawanEntity create(TbKaryawanEntity entity) {
        // Persist Karyawan first to get ID
        super.create(entity);

        // Auto-create User
        // Use email as username if available, otherwise generate one
        String username = entity.getEmail();
        if (username == null || username.isEmpty()) {
            username = entity.getNamaKaryawan().toLowerCase().replaceAll("\\s+", "")
                    .concat(String.valueOf(entity.getId()));
        }

        // Ensure username is unique (basic check, though DB has unique constraint)
        if (userRepository.findByUsername(username).isPresent()) {
            // Fallback if email is used but already taken by another user (unlikely if
            // email is validated, but valid for safety)
            // or if generated username collision
            if (entity.getEmail() == null || entity.getEmail().isEmpty()) {
                username = username + System.currentTimeMillis();
            }
        }

        com.github.b3kt.infrastructure.persistence.entity.UserEntity user = new com.github.b3kt.infrastructure.persistence.entity.UserEntity();
        user.setUsername(username);
        user.setEmail(entity.getEmail() != null ? entity.getEmail() : username + "@example.com"); // Fallback email
        user.setPasswordHash("password"); // Default password, plain text as per AuthServiceImpl logic
        user.setKaryawanId(entity.getId());
        user.setActive(true);

        java.util.List<String> rolesToAssign = entity.getRoles();
        if (rolesToAssign == null || rolesToAssign.isEmpty()) {
            rolesToAssign = java.util.Collections.singletonList("user");
        }

        java.util.Set<com.github.b3kt.infrastructure.persistence.entity.RoleEntity> roleEntities = new java.util.HashSet<>();
        for (String roleName : rolesToAssign) {
            roleRepository.findByName(roleName).ifPresent(roleEntities::add);
        }
        user.setRoles(roleEntities);

        userRepository.persist(user);

        return entity;
    }

    @Override
    protected PanacheRepositoryBase<TbKaryawanEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setRelationships(TbKaryawanEntity entity) {
        if (Objects.nonNull(entity.getIdPosisi())) {
            karyawanPosisiRepository.findByIdOptional(entity.getIdPosisi())
                    .ifPresent(posisi -> entity.setNamePosisi(posisi.getPosisi()));
        }
    }

    @Override
    protected void setEntityId(TbKaryawanEntity entity, Long id) {
        entity.setId(id);
    }

    @Override
    public PageResponse<TbKaryawanEntity> findPaginated(PageRequest pageRequest) {
        PageResponse<TbKaryawanEntity> result;
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
            result = PageHelper.paginate(repository, pageRequest,
                    "lower(namaKaryawan) like ?1 or lower(email) like ?1",
                    "%" + pageRequest.getSearch().toLowerCase() + "%");
        } else {
            result = PageHelper.findAll(repository, pageRequest);
        }
        result.getRows().forEach(this::setRelationships);
        return result;
    }

    public List<TbKaryawanEntity> findAllUnregistered() {
        List<TbKaryawanEntity> list = repository.findAllUnregistered();
        list.forEach(this::setRelationships);
        return list;
    }
}
