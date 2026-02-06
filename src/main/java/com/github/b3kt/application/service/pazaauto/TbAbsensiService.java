package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbAbsensiEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbAbsensiRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@ApplicationScoped
public class TbAbsensiService extends AbstractCrudService<TbAbsensiEntity, Long> {

    @Inject
    TbAbsensiRepository repository;

    @Inject
    TbAbsensiConfigService configService;

    @Override
    protected PanacheRepositoryBase<TbAbsensiEntity, Long> getRepository() {
        return repository;
    }

    @Override
    protected void setEntityId(TbAbsensiEntity entity, Long id) {
        entity.setId(id);
    }

    /**
     * Clock in employee
     */
    @Transactional
    public TbAbsensiEntity clockIn(Long karyawanId, String ipAddress, String deviceInfo, String location) {
        LocalDate today = LocalDate.now();

        // Check if already clocked in today
        TbAbsensiEntity existing = repository.find("karyawanId = ?1 and tanggal = ?2", karyawanId, today).firstResult();
        if (existing != null && existing.getJamMasuk() != null) {
            throw new IllegalStateException("Already clocked in today");
        }

        // Validate IP address if restriction is enabled
        if (isIpRestrictionEnabled() && !isAllowedIp(ipAddress)) {
            throw new SecurityException("Clock-in not allowed from this IP address: " + ipAddress);
        }

        LocalTime now = LocalTime.now();
        LocalTime workStartTime = getWorkStartTime();
        LocalTime lateThreshold = workStartTime.plusMinutes(getLateThresholdMinutes());

        TbAbsensiEntity absensi;
        if (existing != null) {
            absensi = existing;
        } else {
            absensi = new TbAbsensiEntity();
            absensi.setKaryawanId(karyawanId);
            absensi.setTanggal(today);
            absensi.setStatus("HADIR");
        }

        absensi.setJamMasuk(now);
        absensi.setIpMasuk(ipAddress);
        absensi.setDeviceInfo(deviceInfo);
        absensi.setLokasiMasuk(location);
        absensi.setTerlambat(now.isAfter(lateThreshold));

        if (existing == null) {
            repository.persist(absensi);
        }

        return absensi;
    }

    /**
     * Clock out employee
     */
    @Transactional
    public TbAbsensiEntity clockOut(Long karyawanId, String ipAddress, String location) {
        LocalDate today = LocalDate.now();

        TbAbsensiEntity absensi = repository.find("karyawanId = ?1 and tanggal = ?2", karyawanId, today).firstResult();
        if (absensi == null || absensi.getJamMasuk() == null) {
            throw new IllegalStateException("Must clock in before clocking out");
        }

        if (absensi.getJamKeluar() != null) {
            throw new IllegalStateException("Already clocked out today");
        }

        // Validate IP address if restriction is enabled
        if (isIpRestrictionEnabled() && !isAllowedIp(ipAddress)) {
            throw new SecurityException("Clock-out not allowed from this IP address: " + ipAddress);
        }

        LocalTime now = LocalTime.now();
        LocalTime workEndTime = getWorkEndTime();
        LocalTime earlyThreshold = workEndTime.minusMinutes(getEarlyLeaveThresholdMinutes());

        absensi.setJamKeluar(now);
        absensi.setIpKeluar(ipAddress);
        absensi.setLokasiKeluar(location);
        absensi.setPulangCepat(now.isBefore(earlyThreshold));

        // Calculate overtime
        if (now.isAfter(workEndTime)) {
            Duration overtime = Duration.between(workEndTime, now);
            absensi.setLembur((int) overtime.toMinutes());
        }

        return absensi;
    }

    /**
     * Get today's attendance for an employee
     */
    public TbAbsensiEntity getTodayAttendance(Long karyawanId) {
        LocalDate today = LocalDate.now();
        return repository.find("karyawanId = ?1 and tanggal = ?2", karyawanId, today).firstResult();
    }

    /**
     * Get attendance history with pagination and filters
     */
    public PageResponse<TbAbsensiEntity> getAttendanceHistory(
            Long karyawanId, LocalDate startDate, LocalDate endDate,
            String status, PageRequest pageRequest) {

        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        if (karyawanId != null) {
            query.append(" and karyawanId = :karyawanId");
            params.put("karyawanId", karyawanId);
        }

        if (startDate != null) {
            query.append(" and tanggal >= :startDate");
            params.put("startDate", startDate);
        }

        if (endDate != null) {
            query.append(" and tanggal <= :endDate");
            params.put("endDate", endDate);
        }

        if (status != null && !status.isEmpty()) {
            query.append(" and status = :status");
            params.put("status", status);
        }

        // Apply sorting
        Sort sort = pageRequest.isDescending()
                ? Sort.descending(pageRequest.getSortBy() != null ? pageRequest.getSortBy() : "tanggal")
                : Sort.ascending(pageRequest.getSortBy() != null ? pageRequest.getSortBy() : "tanggal");

        long totalCount = repository.count(query.toString(), params);
        List<TbAbsensiEntity> rows = repository.find(query.toString(), sort, params)
                .page(Page.of(pageRequest.getPage() - 1, pageRequest.getRowsPerPage()))
                .list();

        return new PageResponse<>(rows, pageRequest.getPage(), pageRequest.getRowsPerPage(), totalCount);
    }

    /**
     * Get monthly attendance summary
     */
    public Map<String, Object> getMonthlySummary(Long karyawanId, int month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<TbAbsensiEntity> records = repository.find(
                "karyawanId = ?1 and tanggal >= ?2 and tanggal <= ?3",
                karyawanId, startDate, endDate).list();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDays", records.size());
        summary.put("daysPresent", records.stream().filter(r -> "HADIR".equals(r.getStatus())).count());
        summary.put("daysLate", records.stream().filter(r -> Boolean.TRUE.equals(r.getTerlambat())).count());
        summary.put("daysEarlyLeave", records.stream().filter(r -> Boolean.TRUE.equals(r.getPulangCepat())).count());
        summary.put("totalOvertimeMinutes",
                records.stream().mapToInt(r -> r.getLembur() != null ? r.getLembur() : 0).sum());
        summary.put("daysIzin", records.stream().filter(r -> "IZIN".equals(r.getStatus())).count());
        summary.put("daysSakit", records.stream().filter(r -> "SAKIT".equals(r.getStatus())).count());
        summary.put("daysAlpha", records.stream().filter(r -> "ALPHA".equals(r.getStatus())).count());
        summary.put("daysCuti", records.stream().filter(r -> "CUTI".equals(r.getStatus())).count());

        return summary;
    }

    /**
     * Admin marks absence (IZIN, SAKIT, CUTI, ALPHA)
     */
    @Transactional
    public TbAbsensiEntity markAbsence(Long karyawanId, LocalDate tanggal, String status, String keterangan) {
        TbAbsensiEntity absensi = repository.find("karyawanId = ?1 and tanggal = ?2", karyawanId, tanggal)
                .firstResult();

        if (absensi == null) {
            absensi = new TbAbsensiEntity();
            absensi.setKaryawanId(karyawanId);
            absensi.setTanggal(tanggal);
            repository.persist(absensi);
        }

        absensi.setStatus(status);
        absensi.setKeterangan(keterangan);

        return absensi;
    }

    // Configuration helper methods
    private boolean isIpRestrictionEnabled() {
        return configService.getBooleanConfig("ip.restriction.enabled", false);
    }

    private boolean isAllowedIp(String ipAddress) {
        String allowedIps = configService.getStringConfig("allowed.ips", "");
        if (allowedIps.isEmpty()) {
            return true; // If no IPs configured, allow all
        }
        String[] ips = allowedIps.split(",");
        for (String ip : ips) {
            if (ip.trim().equals(ipAddress)) {
                return true;
            }
        }
        return false;
    }

    private LocalTime getWorkStartTime() {
        return LocalTime.parse(configService.getStringConfig("work.start.time", "08:00"));
    }

    private LocalTime getWorkEndTime() {
        return LocalTime.parse(configService.getStringConfig("work.end.time", "17:00"));
    }

    private int getLateThresholdMinutes() {
        return configService.getIntegerConfig("late.threshold.minutes", 15);
    }

    private int getEarlyLeaveThresholdMinutes() {
        return configService.getIntegerConfig("early.leave.threshold.minutes", 15);
    }
}
