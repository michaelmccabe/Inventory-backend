-- Add unique constraint to item name
ALTER TABLE items ADD CONSTRAINT uk_items_name UNIQUE (name);
