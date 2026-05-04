-- V101: Add content_source column to programs table
-- Distinguishes AI_GENERATED, UPLOADED, and MANUAL content.
-- Existing rows are backfilled to AI_GENERATED.

ALTER TABLE programs
    ADD COLUMN content_source VARCHAR(20) NOT NULL DEFAULT 'AI_GENERATED';

UPDATE programs
SET content_source = 'AI_GENERATED'
WHERE content_source IS NULL;
