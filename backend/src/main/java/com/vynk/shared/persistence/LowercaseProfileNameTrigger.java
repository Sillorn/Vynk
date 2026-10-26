package com.vynk.shared.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import org.h2.api.Trigger;

public class LowercaseProfileNameTrigger implements Trigger {

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type) {
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        if (newRow != null) {
            if (newRow.length > 3 && newRow[3] instanceof String firstName) {
                newRow[4] = firstName.toLowerCase();
            }
            if (newRow.length > 4 && newRow[4] instanceof String lastName) {
                newRow[5] = lastName.toLowerCase();
            }
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void remove() {
    }
}
