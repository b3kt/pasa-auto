package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.SpkDetailDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = PazaAutoMapperConfig.class)
public interface SpkDetailMapper {

    SpkDetailMapper INSTANCE = Mappers.getMapper(SpkDetailMapper.class);

    @Mapping(target = "namaItem", source = "id.namaJasa")
    @Mapping(target = "noSpk", source = "id.noSpk")
    SpkDetailDto toDto(TbSpkDetailEntity entity);

    // The composite id (noSpk + namaJasa) is the row's real identity, but the DTO
    // carries it as flat fields (namaItem doubles as the key discriminator — see
    // SpkDetailDto.namaItem). Map them into the embedded id explicitly since
    // MapStruct won't infer a nested-id mapping on its own.
    @Mapping(target = "id.noSpk", source = "noSpk")
    @Mapping(target = "id.namaJasa", source = "namaItem")
    TbSpkDetailEntity toEntity(SpkDetailDto dto);

    List<SpkDetailDto> toDtoList(List<TbSpkDetailEntity> entities);
}