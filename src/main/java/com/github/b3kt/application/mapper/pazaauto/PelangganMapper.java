package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.PelangganDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = PazaAutoMapperConfig.class)
public interface PelangganMapper {

    PelangganMapper INSTANCE = Mappers.getMapper(PelangganMapper.class);

    // The landline (telepon/noTelepon) is deprecated (ADR-0012) and never returned.
    @Mapping(target = "telepon", ignore = true)
    @Mapping(target = "noTelepon", ignore = true)
    PelangganDto toDto(TbPelangganEntity entity);

    // Provenance, merge state and the contact-source flag are owned by the server, never by the
    // request body; the deprecated landline is not accepted.
    @Mapping(target = "sumber", ignore = true)
    @Mapping(target = "mergedIntoId", ignore = true)
    @Mapping(target = "contactFromAccount", ignore = true)
    @Mapping(target = "telepon", ignore = true)
    @Mapping(target = "noTelepon", ignore = true)
    TbPelangganEntity toEntity(PelangganDto dto);

    List<PelangganDto> toDtoList(List<TbPelangganEntity> entities);
}