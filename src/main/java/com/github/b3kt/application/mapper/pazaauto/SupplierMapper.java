package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.SupplierDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSupplierEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface SupplierMapper {

    SupplierMapper INSTANCE = Mappers.getMapper(SupplierMapper.class);

    SupplierDto toDto(TbSupplierEntity entity);

    TbSupplierEntity toEntity(SupplierDto dto);

    List<SupplierDto> toDtoList(List<TbSupplierEntity> entities);
}