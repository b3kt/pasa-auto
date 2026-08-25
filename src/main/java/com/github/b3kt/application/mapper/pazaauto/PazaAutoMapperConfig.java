package com.github.b3kt.application.mapper.pazaauto;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
    componentModel = "jakarta",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PazaAutoMapperConfig {
}
