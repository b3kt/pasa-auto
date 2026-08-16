package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.SpkDetailDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface SpkDetailMapper {

    SpkDetailMapper INSTANCE = Mappers.getMapper(SpkDetailMapper.class);

    SpkDetailDto toDto(TbSpkDetailEntity entity);

    TbSpkDetailEntity toEntity(SpkDetailDto dto);

    List<SpkDetailDto> toDtoList(List<TbSpkDetailEntity> entities);
}