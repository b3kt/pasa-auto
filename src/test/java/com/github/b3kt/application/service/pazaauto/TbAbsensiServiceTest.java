package com.github.b3kt.application.service.pazaauto;

import com.github.b3kt.application.dto.PageRequest;
import com.github.b3kt.application.dto.PageResponse;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbAbsensiEntity;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbAbsensiRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TbAbsensiService Tests")
class TbAbsensiServiceTest {

    @Mock TbAbsensiRepository repository;
    @Mock TbAbsensiConfigService configService;
    @Mock PanacheQuery<TbAbsensiEntity> panacheQuery;

    @InjectMocks TbAbsensiService absensiService;

    @BeforeEach
    void setUp() {
        lenient().when(configService.getBooleanConfig("ip.restriction.enabled", false)).thenReturn(false);
        lenient().when(configService.getStringConfig("work.start.time", "08:00")).thenReturn("08:00");
        lenient().when(configService.getStringConfig("work.end.time", "17:00")).thenReturn("17:00");
        lenient().when(configService.getIntegerConfig("late.threshold.minutes", 15)).thenReturn(15);
        lenient().when(configService.getIntegerConfig("early.leave.threshold.minutes", 15)).thenReturn(15);
        lenient().when(configService.getStringConfig("allowed.ips", "")).thenReturn("");
    }

    private void stubFindByKaryawanAndDate(TbAbsensiEntity result) {
        when(repository.find(eq("karyawanId = ?1 and tanggal = ?2"), any(Long.class), any(LocalDate.class))).thenReturn(panacheQuery);
        when(panacheQuery.firstResult()).thenReturn(result);
    }

    @Test
    @DisplayName("clockIn creates new record when no existing attendance")
    void testClockIn_success_newRecord() {
        stubFindByKaryawanAndDate(null);

        ArgumentCaptor<TbAbsensiEntity> captor = ArgumentCaptor.forClass(TbAbsensiEntity.class);

        TbAbsensiEntity result = absensiService.clockIn(1L, "192.168.1.1", "iPhone", "Office");

        assertNotNull(result);
        assertEquals(1L, result.getKaryawanId());
        assertEquals(LocalDate.now(), result.getTanggal());
        assertEquals("HADIR", result.getStatus());
        assertNotNull(result.getJamMasuk());
        assertEquals("192.168.1.1", result.getIpMasuk());
        assertEquals("iPhone", result.getDeviceInfo());
        assertEquals("Office", result.getLokasiMasuk());
        verify(repository).persist(captor.capture());
        assertNotNull(captor.getValue().getJamMasuk());
    }

    @Test
    @DisplayName("clockIn updates existing record without jamMasuk")
    void testClockIn_success_existingWithoutJamMasuk() {
        TbAbsensiEntity existing = new TbAbsensiEntity();
        existing.setKaryawanId(1L);
        existing.setTanggal(LocalDate.now());
        existing.setJamMasuk(null);

        stubFindByKaryawanAndDate(existing);

        TbAbsensiEntity result = absensiService.clockIn(1L, "192.168.1.1", "iPhone", "Office");

        assertNotNull(result.getJamMasuk());
        verify(repository, never()).persist(any(TbAbsensiEntity.class));
    }

    @Test
    @DisplayName("clockIn throws when already clocked in")
    void testClockIn_alreadyClockedIn() {
        TbAbsensiEntity existing = new TbAbsensiEntity();
        existing.setKaryawanId(1L);
        existing.setTanggal(LocalDate.now());
        existing.setJamMasuk(LocalTime.of(8, 0));

        stubFindByKaryawanAndDate(existing);

        assertThrows(IllegalStateException.class,
                () -> absensiService.clockIn(1L, "192.168.1.1", "iPhone", "Office"));
    }

    @Test
    @DisplayName("clockIn throws SecurityException when IP not allowed")
    void testClockIn_ipRestriction() {
        when(configService.getBooleanConfig("ip.restriction.enabled", false)).thenReturn(true);
        when(configService.getStringConfig("allowed.ips", "")).thenReturn("192.168.1.1");

        stubFindByKaryawanAndDate(null);

        SecurityException ex = assertThrows(SecurityException.class,
                () -> absensiService.clockIn(1L, "10.0.0.1", "iPhone", "Office"));
        assertTrue(ex.getMessage().contains("10.0.0.1"));
    }

    @Test
    @DisplayName("clockOut sets jamKeluar on existing attendance")
    void testClockOut_success() {
        TbAbsensiEntity existing = new TbAbsensiEntity();
        existing.setKaryawanId(1L);
        existing.setTanggal(LocalDate.now());
        existing.setJamMasuk(LocalTime.of(8, 0));
        existing.setJamKeluar(null);

        stubFindByKaryawanAndDate(existing);

        TbAbsensiEntity result = absensiService.clockOut(1L, "192.168.1.1", "Office");

        assertNotNull(result.getJamKeluar());
        assertEquals("192.168.1.1", result.getIpKeluar());
        assertEquals("Office", result.getLokasiKeluar());
    }

    @Test
    @DisplayName("clockOut throws when no attendance record found")
    void testClockOut_notClockedIn() {
        stubFindByKaryawanAndDate(null);

        assertThrows(IllegalStateException.class,
                () -> absensiService.clockOut(1L, "192.168.1.1", "Office"));
    }

    @Test
    @DisplayName("clockOut throws when already clocked out")
    void testClockOut_alreadyClockedOut() {
        TbAbsensiEntity existing = new TbAbsensiEntity();
        existing.setKaryawanId(1L);
        existing.setTanggal(LocalDate.now());
        existing.setJamMasuk(LocalTime.of(8, 0));
        existing.setJamKeluar(LocalTime.of(17, 0));

        stubFindByKaryawanAndDate(existing);

        assertThrows(IllegalStateException.class,
                () -> absensiService.clockOut(1L, "192.168.1.1", "Office"));
    }

    @Test
    @DisplayName("getTodayAttendance returns today's record")
    void testGetTodayAttendance() {
        TbAbsensiEntity expected = new TbAbsensiEntity();
        expected.setKaryawanId(1L);
        expected.setTanggal(LocalDate.now());

        stubFindByKaryawanAndDate(expected);

        TbAbsensiEntity result = absensiService.getTodayAttendance(1L);

        assertNotNull(result);
        assertEquals(1L, result.getKaryawanId());
    }

    @Test
    @DisplayName("getMonthlySummary returns correct counts")
    void testGetMonthlySummary() {
        TbAbsensiEntity hadir = new TbAbsensiEntity();
        hadir.setStatus("HADIR");
        hadir.setTerlambat(false);
        hadir.setPulangCepat(false);
        hadir.setLembur(0);

        TbAbsensiEntity late = new TbAbsensiEntity();
        late.setStatus("HADIR");
        late.setTerlambat(true);
        late.setPulangCepat(false);
        late.setLembur(30);

        TbAbsensiEntity izin = new TbAbsensiEntity();
        izin.setStatus("IZIN");
        izin.setTerlambat(false);
        izin.setPulangCepat(false);
        izin.setLembur(0);

        PanacheQuery<TbAbsensiEntity> listQuery = mock(PanacheQuery.class);
        when(repository.find(anyString(), any(Long.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(listQuery);
        when(listQuery.list()).thenReturn(List.of(hadir, late, izin));

        Map<String, Object> summary = absensiService.getMonthlySummary(1L, 1, 2024);

        assertEquals(3, summary.get("totalDays"));
        assertEquals(2L, summary.get("daysPresent"));
        assertEquals(1L, summary.get("daysLate"));
        assertEquals(0L, summary.get("daysEarlyLeave"));
        assertEquals(30, summary.get("totalOvertimeMinutes"));
        assertEquals(1L, summary.get("daysIzin"));
    }

    @Test
    @DisplayName("markAbsence creates new record when none exists")
    void testMarkAbsence_new() {
        when(repository.find(eq("karyawanId = ?1 and tanggal = ?2"), any(Long.class), any(LocalDate.class))).thenReturn(panacheQuery);
        when(panacheQuery.firstResult()).thenReturn(null);

        LocalDate tanggal = LocalDate.of(2024, 1, 15);
        ArgumentCaptor<TbAbsensiEntity> captor = ArgumentCaptor.forClass(TbAbsensiEntity.class);

        TbAbsensiEntity result = absensiService.markAbsence(1L, tanggal, "IZIN", "Cuti tahunan");

        assertEquals("IZIN", result.getStatus());
        assertEquals("Cuti tahunan", result.getKeterangan());
        verify(repository).persist(captor.capture());
        assertEquals(1L, captor.getValue().getKaryawanId());
        assertEquals(tanggal, captor.getValue().getTanggal());
    }

    @Test
    @DisplayName("markAbsence updates existing record")
    void testMarkAbsence_existing() {
        TbAbsensiEntity existing = new TbAbsensiEntity();
        existing.setKaryawanId(1L);
        existing.setTanggal(LocalDate.of(2024, 1, 15));
        existing.setStatus("HADIR");

        when(repository.find(eq("karyawanId = ?1 and tanggal = ?2"), any(Long.class), any(LocalDate.class))).thenReturn(panacheQuery);
        when(panacheQuery.firstResult()).thenReturn(existing);

        TbAbsensiEntity result = absensiService.markAbsence(1L, LocalDate.of(2024, 1, 15), "SAKIT", "Demam");

        assertEquals("SAKIT", result.getStatus());
        assertEquals("Demam", result.getKeterangan());
        verify(repository, never()).persist(any(TbAbsensiEntity.class));
    }

    @Test
    @DisplayName("getAttendanceHistory returns paginated results")
    void testGetAttendanceHistory() {
        TbAbsensiEntity entity = new TbAbsensiEntity();
        entity.setKaryawanId(1L);
        entity.setTanggal(LocalDate.of(2024, 1, 10));

        PageRequest pr = new PageRequest(1, 10);
        pr.setDescending(true);

        when(repository.count(anyString(), any(java.util.HashMap.class))).thenReturn(1L);
        when(repository.find(anyString(), any(Sort.class), any(java.util.HashMap.class))).thenReturn(panacheQuery);
        when(panacheQuery.page(any(Page.class))).thenReturn(panacheQuery);
        when(panacheQuery.list()).thenReturn(List.of(entity));

        PageResponse<TbAbsensiEntity> result = absensiService.getAttendanceHistory(
                1L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "HADIR", pr);

        assertEquals(1, result.getRowsNumber());
        assertEquals(1L, result.getRows().get(0).getKaryawanId());
    }

    @Test
    @DisplayName("clockOut throws when existing record has null jamMasuk")
    void testClockOut_existingNullJamMasuk() {
        TbAbsensiEntity existing = new TbAbsensiEntity();
        existing.setKaryawanId(1L);
        existing.setTanggal(LocalDate.now());
        existing.setJamMasuk(null);

        stubFindByKaryawanAndDate(existing);

        assertThrows(IllegalStateException.class,
                () -> absensiService.clockOut(1L, "192.168.1.1", "Office"));
    }

    @Test
    @DisplayName("clockOut throws when IP not allowed")
    void testClockOut_ipRestriction() {
        when(configService.getBooleanConfig("ip.restriction.enabled", false)).thenReturn(true);
        when(configService.getStringConfig("allowed.ips", "")).thenReturn("192.168.1.1");

        TbAbsensiEntity existing = new TbAbsensiEntity();
        existing.setKaryawanId(1L);
        existing.setTanggal(LocalDate.now());
        existing.setJamMasuk(LocalTime.of(8, 0));
        existing.setJamKeluar(null);

        stubFindByKaryawanAndDate(existing);

        assertThrows(SecurityException.class,
                () -> absensiService.clockOut(1L, "10.0.0.1", "Office"));
    }

    @Test
    @DisplayName("clockIn does not throw when IP is allowed with restriction enabled")
    void testClockIn_ipAllowed() {
        when(configService.getBooleanConfig("ip.restriction.enabled", false)).thenReturn(true);
        when(configService.getStringConfig("allowed.ips", "")).thenReturn("192.168.1.1");

        stubFindByKaryawanAndDate(null);

        TbAbsensiEntity result = absensiService.clockIn(1L, "192.168.1.1", "iPhone", "Office");

        assertNotNull(result);
        assertEquals("192.168.1.1", result.getIpMasuk());
    }

    @Test
    @DisplayName("getAttendanceHistory with null karyawanId skips filter")
    void testGetAttendanceHistory_nullKaryawanId() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setDescending(true);

        when(repository.count(anyString(), any(java.util.HashMap.class))).thenReturn(0L);
        when(repository.find(anyString(), any(Sort.class), any(java.util.HashMap.class))).thenReturn(panacheQuery);
        when(panacheQuery.page(any(Page.class))).thenReturn(panacheQuery);
        when(panacheQuery.list()).thenReturn(List.of());

        PageResponse<TbAbsensiEntity> result = absensiService.getAttendanceHistory(
                null, null, null, null, pr);

        assertNotNull(result);
    }

    @Test
    @DisplayName("getAttendanceHistory ascending sort with custom sortBy")
    void testGetAttendanceHistory_ascendingSort() {
        PageRequest pr = new PageRequest(1, 10);
        pr.setDescending(false);
        pr.setSortBy("jamMasuk");

        when(repository.count(anyString(), any(java.util.HashMap.class))).thenReturn(0L);
        when(repository.find(anyString(), any(Sort.class), any(java.util.HashMap.class))).thenReturn(panacheQuery);
        when(panacheQuery.page(any(Page.class))).thenReturn(panacheQuery);
        when(panacheQuery.list()).thenReturn(List.of());

        PageResponse<TbAbsensiEntity> result = absensiService.getAttendanceHistory(
                null, null, null, null, pr);

        assertNotNull(result);
    }

    @Test
    @DisplayName("getMonthlySummary includes SAKIT, ALPHA, CUTI counts")
    void testGetMonthlySummary_sakitAlphaCuti() {
        TbAbsensiEntity hadir = new TbAbsensiEntity();
        hadir.setStatus("HADIR");
        hadir.setTerlambat(false);
        hadir.setPulangCepat(false);
        hadir.setLembur(null);

        TbAbsensiEntity sakit = new TbAbsensiEntity();
        sakit.setStatus("SAKIT");
        sakit.setLembur(null);

        TbAbsensiEntity alpha = new TbAbsensiEntity();
        alpha.setStatus("ALPHA");
        alpha.setLembur(null);

        TbAbsensiEntity cuti = new TbAbsensiEntity();
        cuti.setStatus("CUTI");
        cuti.setLembur(null);

        PanacheQuery<TbAbsensiEntity> listQuery = mock(PanacheQuery.class);
        when(repository.find(anyString(), any(Long.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(listQuery);
        when(listQuery.list()).thenReturn(List.of(hadir, sakit, alpha, cuti));

        Map<String, Object> summary = absensiService.getMonthlySummary(1L, 1, 2024);

        assertEquals(4, summary.get("totalDays"));
        assertEquals(1L, summary.get("daysPresent"));
        assertEquals(1L, summary.get("daysSakit"));
        assertEquals(1L, summary.get("daysAlpha"));
        assertEquals(1L, summary.get("daysCuti"));
        assertEquals(0, summary.get("totalOvertimeMinutes"));
    }
}
