/*
 * This file is generated by jOOQ.
 */
package net.forgecraft.services.ember.db.schema.tables.records;


import java.time.LocalDateTime;

import net.forgecraft.services.ember.db.schema.tables.AuditLog;

import org.jooq.JSONB;
import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class AuditLogRecord extends UpdatableRecordImpl<AuditLogRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>audit_log.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>audit_log.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>audit_log.user_id</code>.
     */
    public void setUserId(Long value) {
        set(1, value);
    }

    /**
     * Getter for <code>audit_log.user_id</code>.
     */
    public Long getUserId() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>audit_log.action_type</code>.
     */
    public void setActionType(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>audit_log.action_type</code>.
     */
    public String getActionType() {
        return (String) get(2);
    }

    /**
     * Setter for <code>audit_log.data</code>.
     */
    public void setData(JSONB value) {
        set(3, value);
    }

    /**
     * Getter for <code>audit_log.data</code>.
     */
    public JSONB getData() {
        return (JSONB) get(3);
    }

    /**
     * Setter for <code>audit_log.created_at</code>.
     */
    public void setCreatedAt(LocalDateTime value) {
        set(4, value);
    }

    /**
     * Getter for <code>audit_log.created_at</code>.
     */
    public LocalDateTime getCreatedAt() {
        return (LocalDateTime) get(4);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AuditLogRecord
     */
    public AuditLogRecord() {
        super(AuditLog.AUDIT_LOG);
    }

    /**
     * Create a detached, initialised AuditLogRecord
     */
    public AuditLogRecord(Integer id, Long userId, String actionType, JSONB data, LocalDateTime createdAt) {
        super(AuditLog.AUDIT_LOG);

        setId(id);
        setUserId(userId);
        setActionType(actionType);
        setData(data);
        setCreatedAt(createdAt);
        resetChangedOnNotNull();
    }
}