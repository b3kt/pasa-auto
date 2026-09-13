package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.PenjualanDetailDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanDetailEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = PazaAutoMapperConfig.class)
public interface PenjualanDetailMapper {

    PenjualanDetailMapper INSTANCE = Mappers.getMapper(PenjualanDetailMapper.class);

    PenjualanDetailDto toDto(TbPenjualanDetailEntity entity);

    TbPenjualanDetailEntity toEntity(PenjualanDetailDto dto);

    List<PenjualanDetailDto> toDtoList(List<TbPenjualanDetailEntity> entities);
}