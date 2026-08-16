package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.KaryawanDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface KaryawanMapper {

    KaryawanMapper INSTANCE = Mappers.getMapper(KaryawanMapper.class);

    KaryawanDto toDto(TbKaryawanEntity entity);

    TbKaryawanEntity toEntity(KaryawanDto dto);

    List<KaryawanDto> toDtoList(List<TbKaryawanEntity> entities);
}