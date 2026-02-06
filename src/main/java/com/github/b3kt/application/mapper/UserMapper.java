package com.github.b3kt.application.mapper;

import com.github.b3kt.application.dto.UserInfo;
import com.github.b3kt.domain.model.User;
import com.github.b3kt.infrastructure.persistence.entity.RoleEntity;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper for converting between domain entities and DTOs.
 */
public class UserMapper {

    public static UserInfo toUserInfo(User user) {
        if (user == null) {
            return null;
        }
        return new UserInfo(
                user.getUsername(),
                user.getEmail(),
                convertRoles(user.getRoles()),
                user.getKaryawanId(),
                user.getKaryawanNama());
    }

    private static Set<String> convertRoles(Set<RoleEntity> entities) {
        if(Objects.nonNull(entities)) {
            return entities.stream()
                    .map(RoleEntity::getName)
                    .collect(Collectors.toSet());
        }
        return Set.of();

    }

    private UserMapper() {
        /* This utility class should not be instantiated */
    }
}
