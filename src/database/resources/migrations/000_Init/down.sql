DROP TABLE IF EXISTS discord_users;

DROP TABLE IF EXISTS mods;

DROP TABLE IF EXISTS mod_owners;
DROP INDEX IF EXISTS mod_owners_by_discord_user;
DROP INDEX IF EXISTS mod_owners_by_mod_id;

DROP TABLE IF EXISTS mod_files;
DROP INDEX IF EXISTS mod_files_by_mod_id;
DROP INDEX IF EXISTS mod_files_by_uploader_id;
DROP INDEX IF EXISTS mod_files_by_active;

DROP TABLE IF EXISTS audit_log;
DROP INDEX IF EXISTS audit_log_by_mod_id;
