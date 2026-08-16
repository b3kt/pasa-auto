package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.KaryawanPosisiDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbKaryawanPosisiEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface KaryawanPosisiMapper {

    KaryawanPosisiMapper INSTANCE = Mappers.getMapper(KaryawanPosisiMapper.class);

    KaryawanPosisiDto toDto(TbKaryawanPosisiEntity entity);

    TbKaryawanPosisiEntity toEntity(KaryawanPosisiDto dto);

    List<KaryawanPosisiDto> toDtoList(List<TbKaryawanPosisiEntity> entities);
}