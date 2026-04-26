-- Fix supplier sequence to sync with existing data
-- This will update the sequence to the next available ID after the max existing ID

DO $$
BEGIN
    -- Check if sequence exists and update it
    IF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_name = 'tb_supplier_id_seq') THEN
        PERFORM setval('tb_supplier_id_seq', (SELECT COALESCE(MAX(id), 0) FROM tb_supplier) + 1, false);
        RAISE NOTICE 'Supplier sequence updated to next available ID';
    ELSE
        RAISE NOTICE 'Supplier sequence does not exist';
    END IF;
END $$;
