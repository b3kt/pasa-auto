package com.github.b3kt.application.dto.pazaauto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Totals of the rekap penjualan report, over every row the current filters select - not just the
 * page being displayed.
 */
@Data
@RegisterForReflection
public class RekapPenjualanSummaryDto {

    private long totalSpk;
    private long totalPelanggan;
    private long totalKendaraan;
    private BigDecimal totalDibayar = BigDecimal.ZERO;

    /** Average of finishedAt - startedAt, in seconds; null when no row has both timestamps. */
    private Long avgCompletionSeconds;

    /** How many rows the average is based on, so the UI can say what it is averaging. */
    private long completedCount;
}
