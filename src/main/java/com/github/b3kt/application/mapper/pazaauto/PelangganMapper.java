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

    // telepon is a deprecated alias of noTelepon (ADR-0011): echoing noTelepon keeps a read-modify-write
    // round trip from copying the mobile number or a stale legacy value into the landline field.
    @Mapping(target = "telepon", source = "noTelepon")
    PelangganDto toDto(TbPelangganEntity entity);

    // Provenance and merge state are owned by the server, never by the request body.
    @Mapping(target = "sumber", ignore = true)
    @Mapping(target = "mergedIntoId", ignore = true)
    TbPelangganEntity toEntity(PelangganDto dto);

    List<PelangganDto> toDtoList(List<TbPelangganEntity> entities);
}