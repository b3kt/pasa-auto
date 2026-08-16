package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.BarangDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbBarangEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface BarangMapper {

    BarangMapper INSTANCE = Mappers.getMapper(BarangMapper.class);

    BarangDto toDto(TbBarangEntity entity);

    TbBarangEntity toEntity(BarangDto dto);

    List<BarangDto> toDtoList(List<TbBarangEntity> entities);
}