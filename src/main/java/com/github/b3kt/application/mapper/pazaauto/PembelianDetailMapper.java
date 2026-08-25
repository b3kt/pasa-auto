package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.PembelianDetailDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianDetailEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = PazaAutoMapperConfig.class)
public interface PembelianDetailMapper {

    PembelianDetailMapper INSTANCE = Mappers.getMapper(PembelianDetailMapper.class);

    PembelianDetailDto toDto(TbPembelianDetailEntity entity);

    TbPembelianDetailEntity toEntity(PembelianDetailDto dto);

    List<PembelianDetailDto> toDtoList(List<TbPembelianDetailEntity> entities);
}