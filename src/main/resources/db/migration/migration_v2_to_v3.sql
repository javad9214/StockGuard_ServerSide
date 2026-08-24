-- ============================================
-- Migration: barcode as plain indexed data
-- Version: v2 to v3
-- ============================================
-- Barcodes in user_products are searchable data, NOT an identity key: rows are
-- matched by id/serverId, and a user may legitimately hold several rows sharing
-- one barcode (custom product + adopted catalog product, internal codes, ...).
-- Global product identity is enforced on catalog_products.barcode instead.
-- This migration removes every UNIQUE constraint involving user_products.barcode
-- and leaves a plain index for lookups.
--
-- Run once against PostgreSQL. Safe to re-run.

-- Step 1: Ensure the barcode column exists (databases that came through
-- migration_v1_to_v2.sql got the column added later by Hibernate).
ALTER TABLE user_products ADD COLUMN IF NOT EXISTS barcode VARCHAR(255);

-- Step 2: Drop every UNIQUE constraint involving barcode, whatever its name
-- or column set: the original global one (Hibernate UK_... or Postgres
-- user_products_barcode_key) or any interim per-user variant.
DO $$
DECLARE constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        WHERE t.relname = 'user_products'
          AND c.contype = 'u'
          AND pg_get_constraintdef(c.oid) ILIKE '%barcode%'
    LOOP
        EXECUTE format('ALTER TABLE user_products DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

-- Step 3: Plain (non-unique) index for barcode lookups and scans.
CREATE INDEX IF NOT EXISTS idx_user_products_barcode ON user_products(barcode);
