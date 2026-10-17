package com.github.b3kt.application.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

/**
 * Request DTO for updating users assigned to a role.
 */
@RegisterForReflection
public class UpdateRoleUsersRequest {

    private List<Long> userIds;

    public UpdateRoleUsersRequest() {
    }

    public UpdateRoleUsersRequest(List<Long> userIds) {
        this.userIds = userIds;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }
}
