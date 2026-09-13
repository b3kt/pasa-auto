package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.PelangganDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = PazaAutoMapperConfig.class)
public interface PelangganMapper {

    PelangganMapper INSTANCE = Mappers.getMapper(PelangganMapper.class);

    PelangganDto toDto(TbPelangganEntity entity);

    TbPelangganEntity toEntity(PelangganDto dto);

    List<PelangganDto> toDtoList(List<TbPelangganEntity> entities);
}