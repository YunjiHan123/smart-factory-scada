SET @schema_name = DATABASE();

SET @sql = IF(
    (
        SELECT COUNT(*)
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'chatbot_messages'
          AND COLUMN_NAME = 'chart_spec'
    ) = 0,
    'ALTER TABLE chatbot_messages ADD COLUMN chart_spec JSON NULL AFTER referenced_data',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (
        SELECT COUNT(*)
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'chatbot_messages'
          AND COLUMN_NAME = 'image_data'
    ) = 0,
    'ALTER TABLE chatbot_messages ADD COLUMN image_data LONGTEXT NULL AFTER chart_spec',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (
        SELECT COUNT(*)
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'chatbot_messages'
          AND COLUMN_NAME = 'external_sources'
    ) = 0,
    'ALTER TABLE chatbot_messages ADD COLUMN external_sources JSON NULL AFTER image_data',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
