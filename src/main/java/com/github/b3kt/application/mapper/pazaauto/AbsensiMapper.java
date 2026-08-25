package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.AbsensiDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbAbsensiEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = PazaAutoMapperConfig.class)
public interface AbsensiMapper {

    AbsensiMapper INSTANCE = Mappers.getMapper(AbsensiMapper.class);

    @Mapping(target = "namaKaryawan", source = "namaKaryawan")
    AbsensiDto toDto(TbAbsensiEntity entity);

    TbAbsensiEntity toEntity(AbsensiDto dto);

    List<AbsensiDto> toDtoList(List<TbAbsensiEntity> entities);
}