CREATE UNIQUE INDEX IF NOT EXISTS mod_files_by_modid_and_sha512 on mod_files (mod_id, sha_512);
