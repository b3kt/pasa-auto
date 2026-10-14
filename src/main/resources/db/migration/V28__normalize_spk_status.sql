-- Booking conversions used to create SPKs with the customer-portal status "MENUNGGU",
-- while the staff app uses "OPEN" for the same "open, not yet started" state.
-- Unify on OPEN so converted bookings show up in the staff SPK list.
UPDATE tb_spk SET status_spk = 'OPEN' WHERE status_spk = 'MENUNGGU';
