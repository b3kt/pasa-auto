package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.SparepartDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSparepartEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = PazaAutoMapperConfig.class)
public interface SparepartMapper {

    SparepartMapper INSTANCE = Mappers.getMapper(SparepartMapper.class);

    SparepartDto toDto(TbSparepartEntity entity);

    TbSparepartEntity toEntity(SparepartDto dto);

    List<SparepartDto> toDtoList(List<TbSparepartEntity> entities);
}