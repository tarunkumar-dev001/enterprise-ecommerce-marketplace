-- Adds moderation to an existing MySQL products table without rewriting data.
-- New columns are nullable during the backfill so pre-moderation listings retain
-- their trusted, purchasable status; future inserts default to PENDING_REVIEW.
SET @marketplace_schema := DATABASE();

SET @add_status_sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `products` ADD COLUMN `moderation_status` VARCHAR(32) NULL',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = @marketplace_schema AND table_name = 'products'
      AND column_name = 'moderation_status'
);
PREPARE add_status_statement FROM @add_status_sql;
EXECUTE add_status_statement;
DEALLOCATE PREPARE add_status_statement;

SET @add_reason_sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `products` ADD COLUMN `moderation_reason` VARCHAR(1000) NULL',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = @marketplace_schema AND table_name = 'products'
      AND column_name = 'moderation_reason'
);
PREPARE add_reason_statement FROM @add_reason_sql;
EXECUTE add_reason_statement;
DEALLOCATE PREPARE add_reason_statement;

UPDATE `products` SET `moderation_status` = 'APPROVED' WHERE `moderation_status` IS NULL;

ALTER TABLE `products`
    MODIFY COLUMN `moderation_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    MODIFY COLUMN `moderation_reason` VARCHAR(1000) NULL;
