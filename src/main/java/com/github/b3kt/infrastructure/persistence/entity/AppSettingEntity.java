package com.github.b3kt.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Generic, app-wide key/value setting, runtime-toggleable without a redeploy. Same shape as
 * {@code tb_absensi_config}, but not tied to the attendance domain - any feature can add a row here.
 */
@Entity
@Table(name = "app_settings")
@Getter
@Setter
public class AppSettingEntity extends BaseEntity {

    @Column(name = "config_key", length = 100, nullable = false, unique = true)
    private String configKey;

    @Column(name = "config_value", length = 500)
    private String configValue;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "config_type", length = 50)
    private String configType; // TIME, INTEGER, BOOLEAN, STRING, IP_LIST
}
