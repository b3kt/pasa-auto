-- Odometer reading captured by the customer when requesting a service booking.
-- The staff still records the true intake reading when converting to a work order;
-- this value is the customer's own statement and is shown in the review queue.
ALTER TABLE customer_bookings ADD COLUMN IF NOT EXISTS km INTEGER;
