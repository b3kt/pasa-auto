-- Lightweight service-booking request raised by a customer. Deliberately NOT tb_spk: a work
-- order needs staff-only fields (mechanic assignment, km) that don't exist yet at booking time.
-- Staff reviews the booking and converts it into a real tb_spk row once the vehicle arrives.

CREATE TABLE IF NOT EXISTS customer_bookings (
    id                    BIGSERIAL PRIMARY KEY,
    id_customer_account   BIGINT NOT NULL REFERENCES customer_accounts(id),
    -- Denormalized copy of the account's linked pelanggan at booking time, so a later
    -- unlink/relink of the account doesn't retroactively change a historical booking's ownership
    -- (same defensive style as tb_spk.nama_pelanggan).
    id_pelanggan          BIGINT NOT NULL REFERENCES tb_pelanggan(id),
    nopol                 VARCHAR(10),
    requested_date_time   TIMESTAMP NOT NULL,
    service_type          VARCHAR(100),
    description           VARCHAR(1000),
    status                VARCHAR(20) NOT NULL DEFAULT 'REQUESTED', -- REQUESTED, CONFIRMED, CONVERTED, REJECTED, CANCELLED
    staff_note            VARCHAR(500),
    id_resulting_spk      BIGINT REFERENCES tb_spk(id),
    confirmed_at          TIMESTAMP,
    converted_at          TIMESTAMP,
    cancelled_at          TIMESTAMP,
    created_at            TIMESTAMP,
    created_by            VARCHAR(255),
    updated_at            TIMESTAMP,
    updated_by            VARCHAR(255),
    version               INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_customer_bookings_pelanggan ON customer_bookings (id_pelanggan);
CREATE INDEX IF NOT EXISTS idx_customer_bookings_account ON customer_bookings (id_customer_account);
CREATE INDEX IF NOT EXISTS idx_customer_bookings_status ON customer_bookings (status);
