package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.KendaraanDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKendaraanEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = PazaAutoMapperConfig.class)
public interface KendaraanMapper {

    KendaraanMapper INSTANCE = Mappers.getMapper(KendaraanMapper.class);

    KendaraanDto toDto(TbKendaraanEntity entity);

    TbKendaraanEntity toEntity(KendaraanDto dto);

    List<KendaraanDto> toDtoList(List<TbKendaraanEntity> entities);
}