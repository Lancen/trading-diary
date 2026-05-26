ALTER TABLE industry ADD COLUMN pinned TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE industry ADD COLUMN pin_order INT NULL;
ALTER TABLE concept ADD COLUMN pinned TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE concept ADD COLUMN pin_order INT NULL;
CREATE INDEX idx_industry_pinned ON industry (pinned, pin_order);
CREATE INDEX idx_concept_pinned ON concept (pinned, pin_order);
