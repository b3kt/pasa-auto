package com.github.b3kt.application.dto.customer;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

/** Body for rejecting a link request or a booking. */
@Getter
@Setter
@RegisterForReflection
public class ReviewRequest {
    private String note;
}
