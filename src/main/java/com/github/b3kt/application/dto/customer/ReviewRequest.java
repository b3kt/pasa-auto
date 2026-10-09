package com.github.b3kt.application.dto.customer;

import lombok.Getter;
import lombok.Setter;

/** Body for rejecting a link request or a booking. */
@Getter
@Setter
public class ReviewRequest {
    private String note;
}
