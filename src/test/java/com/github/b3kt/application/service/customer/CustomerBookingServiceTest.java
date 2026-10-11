package com.github.b3kt.application.service.customer;

import com.github.b3kt.application.dto.customer.ConvertToSpkRequest;
import com.github.b3kt.application.dto.customer.CreateBookingRequest;
import com.github.b3kt.application.dto.customer.CustomerBookingAdminDto;
import com.github.b3kt.application.dto.customer.CustomerBookingDto;
import com.github.b3kt.application.service.pazaauto.TbSpkService;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerAccountEntity;
import com.github.b3kt.infrastructure.persistence.entity.customer.CustomerBookingEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbPelangganEntity;
import com.github.b3kt.infrastructure.persistence.entity.pazaauto.TbSpkEntity;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerAccountRepository;
import com.github.b3kt.infrastructure.persistence.repository.customer.CustomerBookingRepository;
import com.github.b3kt.infrastructure.persistence.repository.pazaauto.TbPelangganRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomerBookingService Tests")
class CustomerBookingServiceTest {

    @Mock
    CustomerBookingRepository bookingRepository;

    @Mock
    CustomerAccountRepository accountRepository;

    @Mock
    TbPelangganRepository pelangganRepository;

    @Mock
    TbSpkService spkService;

    @InjectMocks
    CustomerBookingService service;

    private CustomerBookingEntity booking(Long id, Long pelangganId, String status) {
        CustomerBookingEntity booking = new CustomerBookingEntity();
        booking.setId(id);
        booking.setCustomerAccountId(5L);
        booking.setPelangganId(pelangganId);
        booking.setNopol("B1234XYZ");
        booking.setRequestedDateTime(LocalDateTime.of(2026, 1, 2, 10, 0));
        booking.setServiceType("Servis Ringan");
        booking.setDescription("ganti oli");
        booking.setStatus(status);
        return booking;
    }

    private CreateBookingRequest createRequest() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setNopol("B1234XYZ");
        request.setRequestedDateTime(LocalDateTime.of(2026, 1, 2, 10, 0));
        request.setServiceType("Servis Ringan");
        request.setDescription("ganti oli");
        return request;
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("persists a REQUESTED booking scoped to the caller")
        void success() {
            CustomerBookingDto dto = service.create(5L, 7L, createRequest());

            assertEquals("B1234XYZ", dto.getNopol());
            assertEquals("REQUESTED", dto.getStatus());
            ArgumentCaptor<CustomerBookingEntity> captor = ArgumentCaptor.forClass(CustomerBookingEntity.class);
            verify(bookingRepository).persist(captor.capture());
            assertEquals(5L, captor.getValue().getCustomerAccountId());
            assertEquals(7L, captor.getValue().getPelangganId());
            assertEquals(CustomerBookingEntity.STATUS_REQUESTED, captor.getValue().getStatus());
        }
    }

    @Nested
    @DisplayName("listForPelanggan")
    class ListForPelangganTests {

        @Test
        @DisplayName("maps every booking of the pelanggan")
        void maps() {
            when(bookingRepository.findByPelangganId(7L))
                    .thenReturn(List.of(booking(1L, 7L, "REQUESTED")));

            List<CustomerBookingDto> dtos = service.listForPelanggan(7L);

            assertEquals(1, dtos.size());
            assertEquals("ganti oli", dtos.get(0).getDescription());
        }
    }

    @Nested
    @DisplayName("getForPelanggan")
    class GetForPelangganTests {

        @Test
        @DisplayName("returns the booking when it belongs to the caller")
        void owned() {
            when(bookingRepository.findByIdOptional(1L)).thenReturn(Optional.of(booking(1L, 7L, "REQUESTED")));

            assertNotNull(service.getForPelanggan(1L, 7L));
        }

        @Test
        @DisplayName("returns null when the booking belongs to another pelanggan")
        void otherPelanggan() {
            when(bookingRepository.findByIdOptional(1L)).thenReturn(Optional.of(booking(1L, 8L, "REQUESTED")));

            assertNull(service.getForPelanggan(1L, 7L));
        }

        @Test
        @DisplayName("returns null when the booking does not exist")
        void missing() {
            when(bookingRepository.findByIdOptional(1L)).thenReturn(Optional.empty());

            assertNull(service.getForPelanggan(1L, 7L));
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTests {

        @Test
        @DisplayName("cancels a requested booking")
        void success() {
            CustomerBookingEntity booking = booking(1L, 7L, "REQUESTED");
            when(bookingRepository.findByIdOptional(1L)).thenReturn(Optional.of(booking));

            service.cancel(1L, 7L);

            assertEquals("CANCELLED", booking.getStatus());
            assertNotNull(booking.getCancelledAt());
        }

        @Test
        @DisplayName("rejects a booking that is not in REQUESTED state")
        void notRequested() {
            CustomerBookingEntity booking = booking(1L, 7L, "CONFIRMED");
            when(bookingRepository.findByIdOptional(1L)).thenReturn(Optional.of(booking));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.cancel(1L, 7L));
            assertEquals("Only a requested booking can be cancelled", ex.getMessage());
        }

        @Test
        @DisplayName("rejects a booking that is not owned by the caller")
        void notOwned() {
            when(bookingRepository.findByIdOptional(1L)).thenReturn(Optional.of(booking(1L, 8L, "REQUESTED")));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.cancel(1L, 7L));
            assertEquals("Booking not found", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("listForAdmin")
    class ListForAdminTests {

        @Test
        @DisplayName("enriches each booking with the customer email and pelanggan name")
        void maps() {
            when(bookingRepository.findByStatus("REQUESTED"))
                    .thenReturn(List.of(booking(1L, 7L, "REQUESTED")));
            CustomerAccountEntity account = new CustomerAccountEntity();
            account.setEmail("budi@test.com");
            when(accountRepository.findByIdOptional(5L)).thenReturn(Optional.of(account));
            TbPelangganEntity p = new TbPelangganEntity();
            p.setNamaPelanggan("Budi Santoso");
            when(pelangganRepository.findByIdCached(7L)).thenReturn(Optional.of(p));

            List<CustomerBookingAdminDto> dtos = service.listForAdmin("REQUESTED");

            assertEquals(1, dtos.size());
            assertEquals("budi@test.com", dtos.get(0).getCustomerEmail());
            assertEquals("Budi Santoso", dtos.get(0).getNamaPelanggan());
        }
    }

    @Nested
    @DisplayName("confirm")
    class ConfirmTests {

        @Test
        @DisplayName("confirms a requested booking and records the staff note")
        void success() {
            CustomerBookingEntity booking = booking(1L, 7L, "REQUESTED");
            when(bookingRepository.findByIdOptional(1L)).thenReturn(Optional.of(booking));

            service.confirm(1L, "dijadwalkan besok");

            assertEquals("CONFIRMED", booking.getStatus());
            assertEquals("dijadwalkan besok", booking.getStaffNote());
            assertNotNull(booking.getConfirmedAt());
        }

        @Test
        @DisplayName("rejects a booking that is not in REQUESTED state")
        void wrongState() {
            when(bookingRepository.findByIdOptional(1L)).thenReturn(Optional.of(booking(1L, 7L, "CONFIRMED")));

            assertThrows(IllegalArgumentException.class, () -> service.confirm(1L, "note"));
        }
    }

    @Nested
    @DisplayName("reject")
    class RejectTests {

        @Test
        @DisplayName("rejects a requested booking and records the staff note")
        void success() {
            CustomerBookingEntity booking = booking(1L, 7L, "REQUESTED");
            when(bookingRepository.findByIdOptional(1L)).thenReturn(Optional.of(booking));

            service.reject(1L, "slot penuh");

            assertEquals("REJECTED", booking.getStatus());
            assertEquals("slot penuh", booking.getStaffNote());
        }

        @Test
        @DisplayName("rejects an unknown booking")
        void unknown() {
            when(bookingRepository.findByIdOptional(1L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.reject(1L, "note"));
        }
    }

    @Nested
    @DisplayName("convertToSpk")
    class ConvertToSpkTests {

        private ConvertToSpkRequest convertRequest() {
            ConvertToSpkRequest request = new ConvertToSpkRequest();
            request.setMekanikId(3L);
            request.setNamaKaryawan("Andi");
            request.setKm(120000);
            return request;
        }

        @Test
        @DisplayName("creates a work order, links it back and marks the booking converted")
        void success() {
            CustomerBookingEntity booking = booking(1L, 7L, "CONFIRMED");
            when(bookingRepository.findByIdOptional(1L)).thenReturn(Optional.of(booking));
            TbPelangganEntity p = new TbPelangganEntity();
            p.setNamaPelanggan("Budi Santoso");
            when(pelangganRepository.findByIdCached(7L)).thenReturn(Optional.of(p));
            when(spkService.generateNextSpkNumber(anyString())).thenReturn("SPK2026010201");
            when(spkService.create(any(TbSpkEntity.class))).thenAnswer(invocation -> {
                TbSpkEntity spk = invocation.getArgument(0);
                spk.setId(99L);
                return spk;
            });

            Long spkId = service.convertToSpk(1L, convertRequest());

            assertEquals(99L, spkId);
            assertEquals("CONVERTED", booking.getStatus());
            assertEquals(99L, booking.getResultingSpkId());
            assertNotNull(booking.getConvertedAt());

            ArgumentCaptor<TbSpkEntity> captor = ArgumentCaptor.forClass(TbSpkEntity.class);
            verify(spkService).create(captor.capture());
            TbSpkEntity spk = captor.getValue();
            assertEquals("Budi Santoso", spk.getNamaPelanggan());
            assertEquals("B1234XYZ", spk.getNopol());
            assertEquals(120000, spk.getKm());
            assertEquals(3L, spk.getMekanikId());
            assertEquals("MENUNGGU", spk.getStatusSpk());
        }

        @Test
        @DisplayName("still converts when the pelanggan record is missing")
        void pelangganMissing() {
            CustomerBookingEntity booking = booking(1L, 7L, "CONFIRMED");
            when(bookingRepository.findByIdOptional(1L)).thenReturn(Optional.of(booking));
            when(pelangganRepository.findByIdCached(7L)).thenReturn(Optional.empty());
            when(spkService.generateNextSpkNumber(anyString())).thenReturn("SPK2026010201");
            when(spkService.create(any(TbSpkEntity.class))).thenAnswer(invocation -> {
                TbSpkEntity spk = invocation.getArgument(0);
                spk.setId(99L);
                return spk;
            });

            assertEquals(99L, service.convertToSpk(1L, convertRequest()));
        }

        @Test
        @DisplayName("rejects a booking that is not confirmed")
        void notConfirmed() {
            when(bookingRepository.findByIdOptional(1L)).thenReturn(Optional.of(booking(1L, 7L, "REQUESTED")));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.convertToSpk(1L, convertRequest()));
            assertEquals("Only a confirmed booking can be converted to a work order", ex.getMessage());
        }

        @Test
        @DisplayName("rejects an unknown booking")
        void unknown() {
            when(bookingRepository.findByIdOptional(1L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.convertToSpk(1L, convertRequest()));
        }
    }
}
