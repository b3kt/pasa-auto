package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.JasaDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbJasaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = PazaAutoMapperConfig.class)
public interface JasaMapper {

    JasaMapper INSTANCE = Mappers.getMapper(JasaMapper.class);

    JasaDto toDto(TbJasaEntity entity);

    TbJasaEntity toEntity(JasaDto dto);

    List<JasaDto> toDtoList(List<TbJasaEntity> entities);
}