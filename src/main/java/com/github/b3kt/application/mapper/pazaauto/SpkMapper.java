package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.SpkDetailDto;
import com.github.b3kt.application.dto.pazaauto.SpkDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.entity.subentity.SpkMekanik;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = PazaAutoMapperConfig.class, uses = {SpkDetailMapper.class})
public interface SpkMapper {

    SpkMapper INSTANCE = Mappers.getMapper(SpkMapper.class);

    @Mappings({
        @Mapping(target = "details", source = "details"),
        @Mapping(target = "mekanikList", source = "mekanikList"),
        @Mapping(target = "startedAt", source = "startedAt"),
        @Mapping(target = "finishedAt", source = "finishedAt")
    })
    SpkDto toDto(TbSpkEntity entity);

    TbSpkEntity toEntity(SpkDto dto);

    List<SpkDto> toDtoList(List<TbSpkEntity> entities);
}