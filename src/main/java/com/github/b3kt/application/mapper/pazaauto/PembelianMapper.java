package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.PembelianDetailDto;
import com.github.b3kt.application.dto.pazaauto.PembelianDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPembelianEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "cdi", uses = {PembelianDetailMapper.class})
public interface PembelianMapper {

    PembelianMapper INSTANCE = Mappers.getMapper(PembelianMapper.class);

    @Mappings({
        @Mapping(target = "details", source = "details"),
        @Mapping(target = "namaSupplier", source = "namaSupplier")
    })
    PembelianDto toDto(TbPembelianEntity entity);

    TbPembelianEntity toEntity(PembelianDto dto);

    List<PembelianDto> toDtoList(List<TbPembelianEntity> entities);
}