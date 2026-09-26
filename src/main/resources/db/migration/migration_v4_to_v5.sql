-- ============================================
-- Migration: catalog_products.image_url -> image_key
-- Version: v4 to v5
-- ============================================
-- The catalog no longer persists full image URLs for its own uploads:
-- CatalogProduct.imageKey holds the MinIO object key and the absolute URL
-- is built at response time from app.base-url. Rows imported from
-- SNAPP/Daryamart keep their external CDN URL in the renamed column --
-- the read path passes http(s) values through unchanged.
--
-- Hibernate (ddl-auto: update) creates the new image_key column on
-- startup; this script preserves the old data whichever ran first.
--
-- Run once against PostgreSQL. Safe to re-run.

-- Case A: only the old column exists -> plain rename keeps all data
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'catalog_products' AND column_name = 'image_url')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'catalog_products' AND column_name = 'image_key') THEN
        ALTER TABLE catalog_products RENAME COLUMN image_url TO image_key;
    END IF;
END $$;

-- Case B: both columns exist (Hibernate already added image_key) -> copy, then drop old
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'catalog_products' AND column_name = 'image_url')
       AND EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'catalog_products' AND column_name = 'image_key') THEN
        UPDATE catalog_products SET image_key = image_url WHERE image_key IS NULL;
        ALTER TABLE catalog_products DROP COLUMN image_url;
    END IF;
END $$;
