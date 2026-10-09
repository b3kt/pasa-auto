package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.ConvertToSpkRequest;
import com.github.b3kt.application.dto.customer.CreateBookingRequest;
import com.github.b3kt.application.dto.customer.CustomerBookingAdminDto;
import com.github.b3kt.application.dto.customer.CustomerBookingDto;
import com.github.b3kt.application.service.pazaauto.TbSpkService;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerBookingEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerBookingRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * A customer's service-booking request, and the staff-side review/conversion into a real
 * {@code tb_spk} work order. Booking is deliberately not a work order itself - it carries none of
 * the staff-only fields (mechanic, km) a work order needs.
 */
@ApplicationScoped
public class CustomerBookingService {

    private static final DateTimeFormatter SPK_DATE_PREFIX = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Inject
    CustomerBookingRepository bookingRepository;

    @Inject
    CustomerAccountRepository accountRepository;

    @Inject
    TbPelangganRepository pelangganRepository;

    @Inject
    TbSpkService spkService;

    @Transactional
    public CustomerBookingDto create(Long customerAccountId, Long pelangganId, CreateBookingRequest request) {
        CustomerBookingEntity booking = new CustomerBookingEntity();
        booking.setCustomerAccountId(customerAccountId);
        booking.setPelangganId(pelangganId);
        booking.setNopol(request.getNopol());
        booking.setRequestedDateTime(request.getRequestedDateTime());
        booking.setServiceType(request.getServiceType());
        booking.setDescription(request.getDescription());
        booking.setStatus(CustomerBookingEntity.STATUS_REQUESTED);
        bookingRepository.persist(booking);
        return toDto(booking);
    }

    public List<CustomerBookingDto> listForPelanggan(Long pelangganId) {
        return bookingRepository.findByPelangganId(pelangganId).stream().map(this::toDto).toList();
    }

    public CustomerBookingDto getForPelanggan(Long id, Long pelangganId) {
        CustomerBookingEntity booking = ownedBookingOrNull(id, pelangganId);
        return booking == null ? null : toDto(booking);
    }

    @Transactional
    public void cancel(Long id, Long pelangganId) {
        CustomerBookingEntity booking = ownedBookingOrNull(id, pelangganId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        if (!CustomerBookingEntity.STATUS_REQUESTED.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Only a requested booking can be cancelled");
        }
        booking.setStatus(CustomerBookingEntity.STATUS_CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
    }

    public List<CustomerBookingAdminDto> listForAdmin(String status) {
        return bookingRepository.findByStatus(status).stream().map(this::toAdminDto).toList();
    }

    @Transactional
    public void confirm(Long id, String staffNote) {
        CustomerBookingEntity booking = requiredStatus(id, CustomerBookingEntity.STATUS_REQUESTED);
        booking.setStatus(CustomerBookingEntity.STATUS_CONFIRMED);
        booking.setStaffNote(staffNote);
        booking.setConfirmedAt(LocalDateTime.now());
    }

    @Transactional
    public void reject(Long id, String staffNote) {
        CustomerBookingEntity booking = requiredStatus(id, CustomerBookingEntity.STATUS_REQUESTED);
        booking.setStatus(CustomerBookingEntity.STATUS_REJECTED);
        booking.setStaffNote(staffNote);
    }

    @Transactional
    public Long convertToSpk(Long id, ConvertToSpkRequest request) {
        CustomerBookingEntity booking = bookingRepository.findByIdOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!CustomerBookingEntity.STATUS_CONFIRMED.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Only a confirmed booking can be converted to a work order");
        }

        String namaPelanggan = pelangganRepository.findByIdCached(booking.getPelangganId())
                .map(p -> p.getNamaPelanggan()).orElse(null);

        TbSpkEntity spk = new TbSpkEntity();
        spk.setNoSpk(spkService.generateNextSpkNumber(SPK_DATE_PREFIX.format(LocalDateTime.now())));
        spk.setTanggalJamSpk(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        spk.setPelangganId(booking.getPelangganId());
        spk.setNamaPelanggan(namaPelanggan);
        spk.setNopol(booking.getNopol());
        spk.setKm(request.getKm());
        spk.setKmSaatIni(request.getKm());
        spk.setMekanikId(request.getMekanikId());
        spk.setNamaKaryawan(request.getNamaKaryawan());
        spk.setMekanikList(List.of());
        spk.setStatusSpk("MENUNGGU");
        spk.setStatus("AKTIF");
        spk.setKeterangan(booking.getDescription());

        TbSpkEntity created = spkService.create(spk);

        booking.setStatus(CustomerBookingEntity.STATUS_CONVERTED);
        booking.setResultingSpkId(created.getId());
        booking.setConvertedAt(LocalDateTime.now());
        return created.getId();
    }

    private CustomerBookingEntity requiredStatus(Long id, String requiredStatus) {
        CustomerBookingEntity booking = bookingRepository.findByIdOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!requiredStatus.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Booking is not in the expected status");
        }
        return booking;
    }

    private CustomerBookingEntity ownedBookingOrNull(Long id, Long pelangganId) {
        CustomerBookingEntity booking = bookingRepository.findByIdOptional(id).orElse(null);
        if (booking == null || !pelangganId.equals(booking.getPelangganId())) {
            return null;
        }
        return booking;
    }

    private CustomerBookingDto toDto(CustomerBookingEntity b) {
        CustomerBookingDto dto = new CustomerBookingDto();
        dto.setId(b.getId());
        dto.setNopol(b.getNopol());
        dto.setRequestedDateTime(b.getRequestedDateTime());
        dto.setServiceType(b.getServiceType());
        dto.setDescription(b.getDescription());
        dto.setStatus(b.getStatus());
        dto.setStaffNote(b.getStaffNote());
        dto.setResultingSpkId(b.getResultingSpkId());
        dto.setCreatedAt(b.getCreatedAt());
        return dto;
    }

    private CustomerBookingAdminDto toAdminDto(CustomerBookingEntity b) {
        CustomerBookingAdminDto dto = new CustomerBookingAdminDto();
        dto.setId(b.getId());
        dto.setCustomerAccountId(b.getCustomerAccountId());
        dto.setPelangganId(b.getPelangganId());
        dto.setNopol(b.getNopol());
        dto.setRequestedDateTime(b.getRequestedDateTime());
        dto.setServiceType(b.getServiceType());
        dto.setDescription(b.getDescription());
        dto.setStatus(b.getStatus());
        dto.setStaffNote(b.getStaffNote());
        dto.setResultingSpkId(b.getResultingSpkId());
        dto.setCreatedAt(b.getCreatedAt());
        accountRepository.findByIdOptional(b.getCustomerAccountId()).ifPresent(a -> dto.setCustomerEmail(a.getEmail()));
        pelangganRepository.findByIdCached(b.getPelangganId()).ifPresent(p -> dto.setNamaPelanggan(p.getNamaPelanggan()));
        return dto;
    }
}
