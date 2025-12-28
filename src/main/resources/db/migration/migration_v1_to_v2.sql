-- ============================================
-- Migration: Product Split (Catalog + User)
-- Version: v1 to v2
-- ============================================

-- Step 1: Create catalog_products table
CREATE TABLE catalog_products (
                                  id BIGSERIAL PRIMARY KEY,
                                  name VARCHAR(255) NOT NULL,
                                  barcode VARCHAR(255) UNIQUE,
                                  description TEXT,
                                  brand VARCHAR(255),
                                  manufacturer VARCHAR(255),
                                  category VARCHAR(255),
                                  subcategory VARCHAR(255),
                                  image_url VARCHAR(500),
                                  suggested_price BIGINT,
                                  unit VARCHAR(50),
                                  tags TEXT,
                                  status VARCHAR(20) NOT NULL DEFAULT 'VERIFIED',
                                  created_by BIGINT,
                                  verified_by BIGINT,
                                  verified_at TIMESTAMP,
                                  normalized_name VARCHAR(255) NOT NULL,
                                  quality_score INTEGER NOT NULL DEFAULT 0,
                                  adoption_count INTEGER NOT NULL DEFAULT 0,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Step 2: Rename products to user_products
ALTER TABLE products RENAME TO user_products;

-- Step 3: Add new columns to user_products
ALTER TABLE user_products
    ADD COLUMN user_id BIGINT,
    ADD COLUMN catalog_product_id BIGINT,
    ADD COLUMN custom_name VARCHAR(255);

-- Step 4: Add foreign keys
ALTER TABLE user_products
    ADD CONSTRAINT fk_user_products_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE user_products
    ADD CONSTRAINT fk_user_products_catalog
        FOREIGN KEY (catalog_product_id) REFERENCES catalog_products(id) ON DELETE SET NULL;

-- Step 5: Create indexes for catalog_products
CREATE INDEX idx_catalog_barcode ON catalog_products(barcode);
CREATE INDEX idx_catalog_status ON catalog_products(status);
CREATE INDEX idx_catalog_normalized_name ON catalog_products(normalized_name);

-- Step 6: Create indexes for user_products
CREATE INDEX idx_user_products_user_id ON user_products(user_id);
CREATE INDEX idx_user_products_catalog_id ON user_products(catalog_product_id);
CREATE INDEX idx_user_products_user_catalog ON user_products(user_id, catalog_product_id);

-- Step 7: Update existing user_products (CHANGE THIS!)
-- TODO: Replace '1' with your actual admin/user ID
UPDATE user_products SET user_id = 1 WHERE user_id IS NULL;

-- Step 8: Make user_id NOT NULL
ALTER TABLE user_products ALTER COLUMN user_id SET NOT NULL;