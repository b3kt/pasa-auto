package com.github.b3kt.infrastructure.persistence.entity.subentity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

@Data
@RegisterForReflection
public class SpkMekanik {
    private Long id;
    private String tugas;
}
