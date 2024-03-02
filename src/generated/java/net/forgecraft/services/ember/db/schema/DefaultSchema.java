/*
 * This file is generated by jOOQ.
 */
package net.forgecraft.services.ember.db.schema;


import java.util.Arrays;
import java.util.List;

import net.forgecraft.services.ember.db.schema.tables.AuditLog;
import net.forgecraft.services.ember.db.schema.tables.DiscordUsers;
import net.forgecraft.services.ember.db.schema.tables.ModFiles;
import net.forgecraft.services.ember.db.schema.tables.ModOwners;
import net.forgecraft.services.ember.db.schema.tables.Mods;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DefaultSchema extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>DEFAULT_SCHEMA</code>
     */
    public static final DefaultSchema DEFAULT_SCHEMA = new DefaultSchema();

    /**
     * The table <code>audit_log</code>.
     */
    public final AuditLog AUDIT_LOG = AuditLog.AUDIT_LOG;

    /**
     * The table <code>discord_users</code>.
     */
    public final DiscordUsers DISCORD_USERS = DiscordUsers.DISCORD_USERS;

    /**
     * The table <code>mod_files</code>.
     */
    public final ModFiles MOD_FILES = ModFiles.MOD_FILES;

    /**
     * The table <code>mod_owners</code>.
     */
    public final ModOwners MOD_OWNERS = ModOwners.MOD_OWNERS;

    /**
     * The table <code>mods</code>.
     */
    public final Mods MODS = Mods.MODS;

    /**
     * No further instances allowed
     */
    private DefaultSchema() {
        super("", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            AuditLog.AUDIT_LOG,
            DiscordUsers.DISCORD_USERS,
            ModFiles.MOD_FILES,
            ModOwners.MOD_OWNERS,
            Mods.MODS
        );
    }
}