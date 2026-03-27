package com.github.b3kt.application.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO for paginated response data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class PageResponse<T> {
    private List<T> rows;
    private int page;
    private int rowsPerPage;
    private long rowsNumber;
}
