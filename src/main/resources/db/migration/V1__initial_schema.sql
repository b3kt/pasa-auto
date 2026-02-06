-- public.flyway_schema_history definition

-- Drop table

-- DROP TABLE public.flyway_schema_history;

-- CREATE TABLE IF NOT EXISTS public.flyway_schema_history (
-- 	installed_rank int4 NOT NULL,
-- 	"version" varchar(50) NULL,
-- 	description varchar(200) NOT NULL,
-- 	"type" varchar(25) NOT NULL,
-- 	script varchar(1000) NOT NULL,
-- 	checksum int4 NULL,
-- 	installed_by varchar(100) NOT NULL,
-- 	installed_on timestamp DEFAULT now() NOT NULL,
-- 	execution_time int4 NOT NULL,
-- 	success bool NOT NULL,
-- 	CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
-- );
-- CREATE INDEX IF NOT EXISTS flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);

-- CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

-- COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';

-- --
-- -- Name: roles; Type: TABLE; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS roles_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS roles (
    active boolean NOT NULL,
    id bigint NOT NULL DEFAULT nextval('roles_id_seq'::regclass),
    name character varying(50) NOT NULL,
    description character varying(255),
    PRIMARY KEY (id),
    UNIQUE (name)
);

-- --
-- -- Name: system_parameters; Type: TABLE; Schema: public; Owner: postgres
-- --
CREATE SEQUENCE IF NOT EXISTS system_parameters_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS system_parameters (
    id bigint NOT NULL DEFAULT nextval(
        'system_parameters_id_seq'::regclass
    ),
    created_at timestamp without time zone,
    created_by character varying,
    updated_at timestamp without time zone,
    updated_by character varying,
    version integer DEFAULT 0,
    name character varying,
    value jsonb,
    PRIMARY KEY (id),
    UNIQUE (name)
);

-- --
-- -- Name: tb_absensi; Type: TABLE; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS tb_absensi_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_absensi (
    id bigint NOT NULL DEFAULT nextval('tb_absensi_id_seq'::regclass),
    karyawan_id bigint NOT NULL,
    tanggal date NOT NULL,
    jam_masuk time without time zone,
    jam_keluar time without time zone,
    status character varying(25) DEFAULT 'HADIR'::character varying NOT NULL,
    keterangan character varying(500),
    terlambat boolean DEFAULT false,
    pulang_cepat boolean DEFAULT false,
    lembur integer DEFAULT 0,
    lokasi_masuk character varying(200),
    lokasi_keluar character varying(200),
    ip_masuk character varying(50),
    ip_keluar character varying(50),
    device_info character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100),
    updated_at timestamp without time zone,
    updated_by character varying(100),
    version integer DEFAULT 0,
    PRIMARY KEY (id)
);

-- --
-- -- Name: tb_absensi_config; Type: TABLE; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS tb_absensi_config_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_absensi_config (
    id bigint NOT NULL DEFAULT nextval(
        'tb_absensi_config_id_seq'::regclass
    ),
    config_key character varying(100) NOT NULL,
    config_value character varying(500),
    description character varying(500),
    config_type character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100),
    updated_at timestamp without time zone,
    updated_by character varying(100),
    version integer DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (config_key)
);

-- --
-- -- Name: tb_barang; Type: TABLE; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS tb_barang_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_barang (
    harga_beli numeric(18, 2),
    harga_jual numeric(18, 2),
    is_active boolean,
    stock integer,
    stok integer,
    stok_minimal integer,
    version integer DEFAULT 0,
    created_at timestamp(6) without time zone,
    id bigint NOT NULL DEFAULT nextval('tb_barang_id_seq'::regclass),
    id_supplier bigint,
    updated_at timestamp(6) without time zone,
    kode_barang character varying(25),
    satuan character varying(25),
    nama_barang character varying(100) NOT NULL,
    keterangan character varying(500),
    created_by character varying(255),
    updated_by character varying(255),
    PRIMARY KEY (id)
);

-- --
-- -- Name: tb_jasa_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS tb_jasa_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_jasa (
    estimasi_waktu integer,
    harga_jasa integer,
    version integer DEFAULT 0,
    created_at timestamp(6) without time zone,
    id bigint DEFAULT nextval('tb_jasa_id_seq'::regclass) NOT NULL,
    updated_at timestamp(6) without time zone,
    nama_jasa character varying(100) NOT NULL,
    deskripsi character varying(500),
    created_by character varying(255),
    updated_by character varying(255),
    PRIMARY KEY (id)
);

-- --
-- -- Name: tb_karyawan; Type: TABLE; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS tb_karyawan_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_karyawan (
    id bigint NOT NULL DEFAULT nextval(
        'tb_karyawan_id_seq'::regclass
    ),
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    version integer DEFAULT 0,
    jenis_kelamin character varying(1),
    posisi_id integer,
    tanggal_bergabung date,
    tanggal_lahir date,
    id_posisi bigint,
    bergabung character varying(10),
    no_tlpn character varying(25),
    no_telepon character varying(25),
    nama_karyawan character varying(30) NOT NULL,
    email character varying(100),
    alamat character varying(500),
    PRIMARY KEY (id)
);

-- --
-- -- Name: tb_karyawan_posisi; Type: TABLE; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS tb_karyawan_posisi_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_karyawan_posisi (
    id bigint NOT NULL DEFAULT nextval(
        'tb_karyawan_posisi_id_seq'::regclass
    ),
    version integer DEFAULT 0,
    created_by character varying(255),
    created_at timestamp(6) without time zone,
    updated_by character varying(255),
    updated_at timestamp(6) without time zone,
    posisi character varying(25) NOT NULL,
    keterangan character varying(255),
    PRIMARY KEY (id)
);

-- --
-- -- Name: tb_kendaraan; Type: TABLE; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS tb_kendaraan_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_kendaraan (
    id bigint NOT NULL DEFAULT nextval(
        'tb_kendaraan_id_seq'::regclass
    ),
    created_by character varying(255),
    created_at timestamp(6) without time zone,
    updated_by character varying(255),
    updated_at timestamp(6) without time zone,
    version integer DEFAULT 0,
    jenis character varying(50) NOT NULL,
    model character varying(50),
    keterangan character varying(500),
    merk character varying(50),
    PRIMARY KEY (id)
);

-- --
-- -- Name: tb_pelanggan; Type: TABLE; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS tb_pelanggan_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_pelanggan (
    id bigint NOT NULL DEFAULT nextval(
        'tb_pelanggan_id_seq'::regclass
    ),
    created_by character varying(255),
    created_at timestamp(6) without time zone,
    updated_by character varying(255),
    updated_at timestamp(6) without time zone,
    version integer DEFAULT 0,
    jenis_kelamin character varying(1),
    tanggal_join date,
    kode_pos character varying(10),
    nopol character varying(10) NOT NULL,
    tlpn character varying(25),
    no_hp character varying(25),
    no_telepon character varying(25),
    contact_person character varying(30),
    jenis character varying(50),
    merk character varying(50) NOT NULL,
    email character varying(100),
    kota character varying(100),
    nama_pelanggan character varying(100) NOT NULL,
    alamat character varying(500),
    keterangan character varying(500),
    PRIMARY KEY (id)
);

-- --
-- -- Name: tb_pembelian_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS tb_pembelian_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_pembelian (
    id bigint DEFAULT nextval(
        'tb_pembelian_id_seq'::regclass
    ) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying,
    updated_at timestamp without time zone,
    updated_by character varying,
    version integer DEFAULT 0,
    no_pembelian character varying(25) NOT NULL,
    no_urut integer,
    tgl_pembelian timestamp without time zone,
    jenis_pembelian character varying(25) NOT NULL,
    jenis_operasional character varying(50),
    kategori_operasional character varying(25),
    id_supplier bigint,
    grand_total numeric(18, 2),
    jenis_pembayaran character varying(25),
    status_pembayaran character varying(25),
    metode_pembayaran character varying(25),
    diskon numeric(18, 2),
    ppn numeric(18, 2),
    keterangan character varying(500),
    id_karyawan bigint,
    PRIMARY KEY (id),
    CONSTRAINT chk_jenis_pembelian CHECK (
        (
            (jenis_pembelian)::text = ANY (
                (
                    ARRAY[
                        'SPAREPART'::character varying,
                        'OPERASIONAL'::character varying
                    ]
                )::text []
            )
        )
    ),
    CONSTRAINT chk_kategori_operasional CHECK (
        (
            (kategori_operasional)::text = ANY (
                (
                    ARRAY[
                        'DAILY'::character varying,
                        'WEEKLY'::character varying,
                        'MONTHLY'::character varying,
                        'YEARLY'::character varying,
                        'ON_DEMAND'::character varying
                    ]
                )::text []
            )
        )
    )
);

-- --
-- -- Name: tb_pembelian_detail_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS tb_pembelian_detail_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_pembelian_detail (
    id bigint DEFAULT nextval(
        'tb_pembelian_detail_id_seq'::regclass
    ) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying,
    updated_at timestamp without time zone,
    updated_by character varying,
    version integer DEFAULT 0,
    id_pembelian bigint NOT NULL,
    nama_item character varying(100) NOT NULL,
    kategori_item character varying(25),
    harga numeric(18, 2) NOT NULL,
    kuantiti integer NOT NULL,
    total numeric(18, 2),
    keterangan character varying(255),
    id_barang bigint,
    id_sparepart bigint,
    id_supplier bigint,
    CONSTRAINT chk_kategori_item CHECK (
        (
            (kategori_item)::text = ANY (
                (
                    ARRAY[
                        'SPAREPART'::character varying,
                        'BARANG'::character varying,
                        'OPERASIONAL'::character varying
                    ]
                )::text []
            )
        )
    ),
    PRIMARY KEY (id)
);

-- --
-- -- Name: tb_pembelian_barang_detail; Type: TABLE; Schema: public; Owner: postgres
-- --

CREATE TABLE IF NOT EXISTS tb_pembelian_barang_detail (
    id uuid NOT NULL,
    created_at timestamp without time zone,
    created_by character varying(255),
    updated_at timestamp without time zone,
    updated_by character varying(255),
    version integer DEFAULT 0,
    no_pembelian character varying(25) NOT NULL,
    nama_barang character varying(50) NOT NULL,
    harga_barang integer NOT NULL,
    kuantiti integer NOT NULL,
    total integer,
    keterangan character varying(255),
    id_barang bigint,
    id_sparepart bigint,
    PRIMARY KEY (id)
);

-- --
-- -- Name: TABLE tb_pembelian_detail; Type: COMMENT; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS tb_penjualan_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_penjualan (
    id bigint DEFAULT nextval(
        'tb_penjualan_id_seq'::regclass
    ) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100),
    updated_at timestamp without time zone,
    updated_by character varying(100),
    version integer DEFAULT 0,
    no_spk character varying NOT NULL,
    no_penjualan character varying(50) NOT NULL,
    grand_total integer,
    diskon numeric(18, 2),
    ppn numeric(18, 2),
    uang_dibayar numeric(18, 2),
    kembalian numeric(18, 2),
    metode_pembayaran character varying(25),
    status_pembayaran character varying(25),
    tgl_jam_penjualan timestamp without time zone,
    finish_at timestamp without time zone,
    paid_at timestamp without time zone,
    id_karyawan bigint,
    id_kendaraan bigint,
    id_pelanggan bigint,
    keterangan character varying(1500),
    PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS tb_penjualan_detail_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_penjualan_detail (
    no_penjualan character varying(50) DEFAULT ''::character varying NOT NULL,
    kategori character varying(10) DEFAULT NULL::character varying,
    nama_jasa_barang character varying(40) DEFAULT ''::character varying NOT NULL,
    harga_jual integer NOT NULL,
    kuantiti integer NOT NULL,
    total integer,
    id bigint NOT NULL DEFAULT nextval(
        'tb_penjualan_detail_id_seq'::regclass
    ),
    diskon numeric(18, 2),
    keterangan character varying(255),
    id_barang bigint,
    id_jasa bigint,
    id_sparepart bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying,
    updated_at timestamp without time zone,
    updated_by character varying,
    version integer DEFAULT 0,
    PRIMARY KEY (id)
);

-- --
-- -- Name: tb_sparepart; Type: TABLE; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS tb_sparepart_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_sparepart (
    id bigint NOT NULL DEFAULT nextval(
        'tb_sparepart_id_seq'::regclass
    ),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying,
    updated_at timestamp without time zone,
    updated_by character varying,
    version integer DEFAULT 0,
    harga_beli numeric(18, 2),
    harga_jual numeric(18, 2),
    is_active boolean NOT NULL,
    stok integer,
    stok_minimal integer,
    id_supplier bigint,
    kd_barang character varying(9) NOT NULL,
    kode_sparepart character varying(25) NOT NULL,
    satuan character varying(25),
    nama_barang character varying(40),
    merek character varying(50),
    tipe_kendaraan character varying(50),
    nama_sparepart character varying(100) NOT NULL,
    keterangan character varying(500),
    PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS tb_spk_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_spk (
    id bigint NOT NULL DEFAULT nextval('tb_spk_id_seq'::regclass),
    created_at timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    version integer DEFAULT 0,
    id_mekanik bigint,
    id_pelanggan bigint,
    mekanik_list jsonb,
    id_cs bigint,
    nopol character varying(10),
    no_spk character varying(30) NOT NULL,
    nama_karyawan character varying(300),
    diskon numeric(18, 2),
    km integer,
    km_saat_ini integer,
    no_antrian integer,
    ppn numeric(18, 2),
    status character varying(25) DEFAULT 'OPEN'::character varying,
    status_spk character varying(25),
    tgl_jam_spk character varying(25),
    started_at timestamp without time zone,
    finished_at timestamp without time zone,
    keluhan character varying(1000),
    keterangan character varying(1000),
    PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS tb_spk_detail_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS tb_spk_detail (
    id bigint DEFAULT nextval(
        'tb_spk_detail_id_seq'::regclass
    ) NOT NULL,
    created_by character varying(255),
    created_at timestamp(6) without time zone,
    updated_by character varying(255),
    updated_at timestamp(6) without time zone,
    version integer DEFAULT 0,
    jumlah integer,
    id_jasa bigint,
    id_sparepart bigint,
    no_spk character varying(11) NOT NULL,
    nama_jasa character varying(40) NOT NULL,
    keterangan character varying(500),
    PRIMARY KEY (id)
);

-- --
-- -- Name: tb_supplier_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS tb_supplier_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- --
-- -- Name: tb_supplier; Type: TABLE; Schema: public; Owner: postgres
-- --

CREATE TABLE IF NOT EXISTS tb_supplier (
    version integer DEFAULT 0,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    kode_pos character varying(10),
    tlpn character varying(13),
    no_hp_kontak character varying(25),
    no_telepon character varying(25),
    detail_supplier character varying(100),
    email character varying(100),
    kontak_person character varying(100),
    kota character varying(100),
    nama_supplier character varying(100) NOT NULL,
    alamat character varying(500),
    keterangan character varying(500),
    created_by character varying(255),
    updated_by character varying(255),
    id bigint DEFAULT nextval(
        'tb_supplier_id_seq'::regclass
    ) NOT NULL,
    PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS tb_sys_parameter_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- --
-- -- Name: user_roles; Type: TABLE; Schema: public; Owner: postgres
-- --

CREATE TABLE IF NOT EXISTS user_roles (
    role_id bigint NOT NULL,
    user_id bigint NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS users_id_seq START
WITH
    1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS users (
    active boolean NOT NULL,
    id bigint NOT NULL DEFAULT nextval('users_id_seq'::regclass),
    username character varying(50) NOT NULL,
    email character varying(100) NOT NULL,
    password_hash character varying(255) NOT NULL,
    karyawan_id bigint DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying,
    updated_at timestamp without time zone,
    updated_by character varying,
    version bigint,
    PRIMARY KEY (id)
);

-- --
-- -- Name: tb_penjualan tb_penjualan_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
-- --

-- --
-- -- Name: idx_absensi_karyawan; Type: INDEX; Schema: public; Owner: postgres
-- --

CREATE INDEX IF NOT EXISTS idx_absensi_karyawan ON tb_absensi USING btree (karyawan_id);

-- --
-- -- Name: idx_absensi_status; Type: INDEX; Schema: public; Owner: postgres
-- --

CREATE INDEX IF NOT EXISTS idx_absensi_status ON tb_absensi USING btree (status);

-- --
-- -- Name: idx_absensi_tanggal; Type: INDEX; Schema: public; Owner: postgres
-- --

CREATE INDEX IF NOT EXISTS idx_absensi_tanggal ON tb_absensi USING btree (tanggal);

-- --
-- -- Name: idx_email; Type: INDEX; Schema: public; Owner: postgres
-- --

CREATE INDEX IF NOT EXISTS idx_email ON users USING btree (email);

-- --
-- -- Name: idx_pembelian_detail_id_pembelian; Type: INDEX; Schema: public; Owner: postgres
-- --

CREATE INDEX IF NOT EXISTS idx_pembelian_detail_id_pembelian ON tb_pembelian_detail USING btree (id_pembelian);

-- --
-- -- Name: idx_pembelian_jenis_operasional; Type: INDEX; Schema: public; Owner: postgres
-- --

CREATE INDEX IF NOT EXISTS idx_pembelian_jenis_operasional ON tb_pembelian USING btree (jenis_operasional);

-- --
-- -- Name: idx_pembelian_jenis_pembelian; Type: INDEX; Schema: public; Owner: postgres
-- --

CREATE INDEX IF NOT EXISTS idx_pembelian_jenis_pembelian ON tb_pembelian USING btree (jenis_pembelian);

-- --
-- -- Name: idx_pembelian_kategori_operasional; Type: INDEX; Schema: public; Owner: postgres
-- --

CREATE INDEX IF NOT EXISTS idx_pembelian_kategori_operasional ON tb_pembelian USING btree (kategori_operasional);

-- --
-- -- Name: idx_pembelian_no_pembelian; Type: INDEX; Schema: public; Owner: postgres
-- --

CREATE UNIQUE INDEX IF NOT EXISTS idx_pembelian_no_pembelian ON tb_pembelian USING btree (no_pembelian);

-- --
-- -- Name: idx_pembelian_tgl_pembelian; Type: INDEX; Schema: public; Owner: postgres
-- --

CREATE INDEX IF NOT EXISTS idx_pembelian_tgl_pembelian ON tb_pembelian USING btree (tgl_pembelian);

-- --
-- -- Name: tb_barang_unique; Type: INDEX; Schema: public; Owner: postgres
-- --

CREATE UNIQUE INDEX IF NOT EXISTS tb_barang_unique ON tb_barang USING btree (nama_barang);

-- --
-- -- Name: tb_pembelian_barang_detail_unique; Type: INDEX; Schema: public; Owner: postgres
-- --

CREATE UNIQUE INDEX IF NOT EXISTS tb_pembelian_barang_detail_unique ON tb_pembelian_barang_detail USING btree (no_pembelian, nama_barang);

-- --
-- -- Name: tb_pembelian_barang_unique; Type: INDEX; Schema: public; Owner: postgres
-- --

-- CREATE UNIQUE INDEX IF NOT EXISTS tb_pembelian_barang_unique ON tb_pembelian_barang USING btree (no_pembelian);

-- --
-- -- Name: ukdaaw2mydabnpcnt6inochoidi; Type: INDEX; Schema: public; Owner: postgres
-- --

CREATE UNIQUE INDEX IF NOT EXISTS ukdaaw2mydabnpcnt6inochoidi ON tb_sparepart USING btree (kode_sparepart);

-- --
-- -- Name: tb_absensi fk_absensi_karyawan; Type: FK CONSTRAINT; Schema: public; Owner: postgres
-- --

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_absensi_karyawan') THEN
        ALTER TABLE tb_absensi
        ADD CONSTRAINT fk_absensi_karyawan FOREIGN KEY (karyawan_id) REFERENCES tb_karyawan (id) ON DELETE CASCADE;

END IF;

END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pembelian_detail_barang') THEN
        ALTER TABLE tb_pembelian_detail
        ADD CONSTRAINT fk_pembelian_detail_barang FOREIGN KEY (id_barang) REFERENCES tb_barang (id) ON DELETE SET NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pembelian_karyawan') THEN
        ALTER TABLE tb_pembelian
        ADD CONSTRAINT fk_pembelian_karyawan FOREIGN KEY (id_karyawan) REFERENCES tb_karyawan (id) ON DELETE SET NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkc3m376fbd9nvrc4h58eliqqlj') THEN
        ALTER TABLE user_roles
        ADD CONSTRAINT fkc3m376fbd9nvrc4h58eliqqlj FOREIGN KEY (user_id) REFERENCES users (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkgaoscqcq4j14rcn9frbqm9mu1') THEN
        ALTER TABLE tb_penjualan_detail
        ADD CONSTRAINT fkgaoscqcq4j14rcn9frbqm9mu1 FOREIGN KEY (id_jasa) REFERENCES tb_jasa (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkptcgvku6hqik9b87ymn2pm0ft') THEN
        ALTER TABLE tb_penjualan_detail
        ADD CONSTRAINT fkptcgvku6hqik9b87ymn2pm0ft FOREIGN KEY (id_barang) REFERENCES tb_barang (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkrrb5jstxo5xn0nomagp6n0oou') THEN
        ALTER TABLE user_roles
        ADD CONSTRAINT fkrrb5jstxo5xn0nomagp6n0oou FOREIGN KEY (role_id) REFERENCES roles (id);
    END IF;
END $$;

-- --
-- -- Name: permissions; Type: TABLE; Schema: public; Owner: postgres
-- --

CREATE SEQUENCE IF NOT EXISTS permissions_id_seq START
WITH
    1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS permissions (
    id bigint NOT NULL DEFAULT nextval('permissions_id_seq'),
    name character varying(100) NOT NULL,
    description character varying(255),
    resource character varying(50) NOT NULL,
    action character varying(50) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    PRIMARY KEY (id),
    CONSTRAINT uk_permissions_name UNIQUE (name)
);

-- CREATE UNIQUE INDEX IF NOT EXISTS idx_permission_name ON permissions (name);

-- CREATE INDEX IF NOT EXISTS idx_permission_resource_action ON permissions (resource, action);

-- --
-- -- Name: role_permissions; Type: TABLE; Schema: public; Owner: postgres
-- --

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id bigint NOT NULL,
    permission_id bigint NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

-- CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions (role_id);

-- CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions (permission_id);

-- --
-- -- PostgreSQL database dump complete
-- --