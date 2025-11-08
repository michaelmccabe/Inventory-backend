-- Remove duplicates keeping only the first occurrence
DELETE FROM items a USING items b
WHERE a.id > b.id AND a.name = b.name;

-- Add unique constraint to item name
ALTER TABLE items ADD CONSTRAINT uk_items_name UNIQUE (name);
