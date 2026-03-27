package com.github.b3kt.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Request DTO for updating users assigned to a role.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleUsersRequest {

    private List<Long> userIds;
}
