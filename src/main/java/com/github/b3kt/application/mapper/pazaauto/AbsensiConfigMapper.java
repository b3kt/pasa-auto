package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.AbsensiConfigDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbAbsensiConfigEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = PazaAutoMapperConfig.class)
public interface AbsensiConfigMapper {

    AbsensiConfigMapper INSTANCE = Mappers.getMapper(AbsensiConfigMapper.class);

    AbsensiConfigDto toDto(TbAbsensiConfigEntity entity);

    TbAbsensiConfigEntity toEntity(AbsensiConfigDto dto);

    List<AbsensiConfigDto> toDtoList(List<TbAbsensiConfigEntity> entities);
}