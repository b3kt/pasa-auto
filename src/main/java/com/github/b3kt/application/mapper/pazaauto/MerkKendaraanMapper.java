package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.MerkKendaraanDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbMerkKendaraanEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = PazaAutoMapperConfig.class)
public interface MerkKendaraanMapper {

    MerkKendaraanDto toDto(TbMerkKendaraanEntity entity);

    TbMerkKendaraanEntity toEntity(MerkKendaraanDto dto);

    List<MerkKendaraanDto> toDtoList(List<TbMerkKendaraanEntity> entities);
}
