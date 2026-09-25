ALTER TABLE orders
    ALTER COLUMN total_amount TYPE NUMERIC(19, 2)
    USING total_amount::NUMERIC(19, 2);

ALTER TABLE order_items
    ALTER COLUMN unit_price TYPE NUMERIC(19, 2)
    USING unit_price::NUMERIC(19, 2);

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_total_amount_non_negative
    CHECK (total_amount >= 0);

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_quantity_positive
    CHECK (quantity > 0);

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_unit_price_positive
    CHECK (unit_price > 0);

CREATE INDEX idx_order_items_order_id
    ON order_items(order_id);