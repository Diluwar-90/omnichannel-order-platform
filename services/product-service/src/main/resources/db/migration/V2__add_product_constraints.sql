ALTER TABLE products
    ALTER COLUMN name SET NOT NULL;

ALTER TABLE products
    ALTER COLUMN price SET NOT NULL;

ALTER TABLE products
    ALTER COLUMN stock_quantity SET NOT NULL;

ALTER TABLE products
    ADD CONSTRAINT chk_products_price_positive
        CHECK (price > 0);

ALTER TABLE products
    ADD CONSTRAINT chk_products_stock_non_negative
        CHECK (stock_quantity >= 0);