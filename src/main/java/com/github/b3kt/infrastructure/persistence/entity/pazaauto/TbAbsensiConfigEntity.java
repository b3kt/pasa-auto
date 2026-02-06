package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.github.b3kt.infrastructure.persistence.entity.BaseEntity;

@Entity
@Table(name = "tb_absensi_config")
@Getter
@Setter
public class TbAbsensiConfigEntity extends BaseEntity {

    @Column(name = "config_key", length = 100, nullable = false, unique = true)
    private String configKey;

    @Column(name = "config_value", length = 500)
    private String configValue;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "config_type", length = 50)
    private String configType; // TIME, INTEGER, BOOLEAN, STRING, IP_LIST
}
