CREATE TABLE IF NOT EXISTS discord_users (
    snowflake BIGINT NOT NULL PRIMARY KEY,
    display_name TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mods (
    id TEXT NOT NULL PRIMARY KEY,
    project_url TEXT,
    issues_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mod_owners (
    id INTEGER PRIMARY KEY,
    mod_id TEXT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY(mod_id) REFERENCES mods(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY(user_id) REFERENCES discord_users(snowflake) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX IF NOT EXISTS mod_owners_by_discord_user ON mod_owners (user_id);
CREATE INDEX IF NOT EXISTS mod_owners_by_mod_id ON mod_owners (mod_id);

CREATE TABLE IF NOT EXISTS mod_files (
    id INTEGER PRIMARY KEY,
    mod_id TEXT NOT NULL,
    uploader_id BIGINT NOT NULL,
    mod_version TEXT NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    file_name TEXT NOT NULL, -- file name as uploaded
    file_path TEXT UNIQUE NOT NULL, -- path to the file in the global file storage
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY(mod_id) REFERENCES mods(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY(uploader_id) REFERENCES discord_users(snowflake) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX IF NOT EXISTS mod_files_by_mod_id ON mod_files (mod_id);
CREATE INDEX IF NOT EXISTS mod_files_by_uploader_id ON mod_files (uploader_id);
CREATE INDEX IF NOT EXISTS mod_files_by_active ON mod_files (active);

-- no ON DELETE CASCADE to keep audit log intact if user is deleted
-- explicitly auto-increment the id, this is different from the default behavior of only requiring an unused row ID.
-- it will not reuse IDs, as well as guarantee that the new ID is greater than the largest previously used ID.
CREATE TABLE IF NOT EXISTS audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT NOT NULL,
    action_type TEXT NOT NULL,
    data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY(user_id) REFERENCES discord_users(snowflake) ON UPDATE CASCADE
);
CREATE INDEX IF NOT EXISTS audit_log_by_user_id ON audit_log (user_id);
