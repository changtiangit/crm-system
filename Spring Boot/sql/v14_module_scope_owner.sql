USE crm;

SET @add_scope_owner = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'crm_module_records' AND column_name = 'scope_owner') = 0,
  'ALTER TABLE crm_module_records ADD COLUMN scope_owner VARCHAR(80) NOT NULL DEFAULT '''' AFTER owner',
  'SELECT 1'
);
PREPARE migration_stmt FROM @add_scope_owner;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

UPDATE crm_module_records
SET scope_owner = owner
WHERE scope_owner = '' AND owner <> '';
