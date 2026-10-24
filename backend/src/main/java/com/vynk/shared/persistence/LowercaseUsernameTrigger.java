package com.vynk.shared.persistence;

import java.sql.Connection;
import java.sql.SQLException;

import org.h2.api.Trigger;

public class LowercaseUsernameTrigger implements Trigger {

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type) {
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        if (newRow != null && newRow.length > 1 && newRow[1] instanceof String username) {
            newRow[2] = username.toLowerCase();
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void remove() {
    }
}
