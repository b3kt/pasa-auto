-- Baseline Migration for Pasa Auto
-- This migration represents the complete database schema as of version 0.0.34
-- For fresh installations, use Flyway baseline: `flyway baseline -baselineVersion=14`
-- For existing installations, this migration will be skipped (already applied V1-V13)

-- =====================================================
-- BASELINE: Current Schema State
-- =====================================================
-- This is a marker migration. The actual schema is defined in V1-V13.
-- For new deployments, run: flyway baseline -baselineVersion=14
-- Then subsequent migrations (V15+) will apply normally.

-- Table list (created by V1-V13):
-- 1. flyway_schema_history (managed by Flyway)
-- 2. users
-- 3. roles
-- 4. permissions
-- 5. user_roles
-- 6. system_parameters
-- 7. tb_pelanggan
-- 8. tb_kendaraan
-- 9. tb_karyawan
-- 10. tb_karyawan_posisi
-- 11. tb_barang
-- 12. tb_sparepart
-- 13. tb_jasa
-- 14. tb_supplier
-- 15. tb_pembelian
-- 16. tb_pembelian_detail
-- 17. tb_penjualan
-- 18. tb_penjualan_detail
-- 19. tb_spk
-- 20. tb_spk_detail
-- 21. tb_absensi
-- 22. tb_absensi_config
-- 23. tb_audit_trail

-- Indexes, constraints, and triggers are defined in V1-V13.

-- =====================================================
-- BASELINE MARKER - No schema changes
-- =====================================================
-- This migration exists to allow baseline for fresh installs
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM public.flyway_schema_history),
    '14',
    'Baseline marker for current schema state',
    'SQL',
    'V14__baseline.sql',
    NULL,
    CURRENT_USER,
    NOW(),
    0,
    TRUE
) ON CONFLICT DO NOTHING;

-- =====================================================
-- Verify all expected tables exist
-- =====================================================
DO $$
DECLARE
    expected_tables TEXT[] := ARRAY[
        'users', 'roles', 'permissions', 'user_roles', 'system_parameters',
        'tb_pelanggan', 'tb_kendaraan', 'tb_karyawan', 'tb_karyawan_posisi',
        'tb_barang', 'tb_sparepart', 'tb_jasa', 'tb_supplier',
        'tb_pembelian', 'tb_pembelian_detail', 'tb_penjualan', 'tb_penjualan_detail',
        'tb_spk', 'tb_spk_detail', 'tb_absensi', 'tb_absensi_config',
        'tb_audit_trail'
    ];
    missing_tables TEXT[] := '{}';
    tbl TEXT;
BEGIN
    FOREACH tbl IN ARRAY expected_tables LOOP
        IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = tbl) THEN
            missing_tables := array_append(missing_tables, tbl);
        END IF;
    END LOOP;
    
    IF array_length(missing_tables, 1) > 0 THEN
        RAISE WARNING 'Missing tables (may be expected for fresh baseline): %', missing_tables;
    ELSE
        RAISE NOTICE 'All expected tables present in baseline';
    END IF;
END $$;