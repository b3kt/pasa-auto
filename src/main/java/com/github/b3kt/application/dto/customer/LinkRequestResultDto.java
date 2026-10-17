package com.github.b3kt.application.dto.customer;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@RegisterForReflection
public class LinkRequestResultDto {

    public static final String STATUS_LINKED = "LINKED";
    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";

    private String status;
    private String message;
}
