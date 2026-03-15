package com.github.b3kt.infrastructure.persistence.entity.pazaauto;

import lombok.Getter;

public enum KategoriOperasional {
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan"),
    YEARLY("Tahunan"),
    ON_DEMAND("Sesuai Kebutuhan");

    @Getter
    private final String label;
    KategoriOperasional(String label){
        this.label = label;
    }
}
