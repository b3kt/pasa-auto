package com.github.b3kt.application.mapper.pazaauto;

import com.github.b3kt.application.dto.pazaauto.PenjualanDetailDto;
import com.github.b3kt.application.dto.pazaauto.PenjualanDto;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanDetailEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPenjualanEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = PazaAutoMapperConfig.class, uses = {PenjualanDetailMapper.class})
public interface PenjualanMapper {

    PenjualanMapper INSTANCE = Mappers.getMapper(PenjualanMapper.class);

    @Mappings({
        @Mapping(target = "details", source = "details"),
        @Mapping(target = "namaPelanggan", source = "namaPelanggan"),
        @Mapping(target = "alamatPelanggan", source = "alamatPelanggan"),
        @Mapping(target = "merkKendaraan", source = "merkKendaraan"),
        @Mapping(target = "jenisKendaraan", source = "jenisKendaraan"),
        @Mapping(target = "noPolisi", source = "noPolisi")
    })
    PenjualanDto toDto(TbPenjualanEntity entity);

    TbPenjualanEntity toEntity(PenjualanDto dto);

    List<PenjualanDto> toDtoList(List<TbPenjualanEntity> entities);
}