package com.vokal.database;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

public abstract class AbstractDataModel {

    private static SQLiteTable mTable;
    private static Uri mContentUri;

    public AbstractDataModel() {}

    public final Uri getContentUri() {
        return mContentUri;
    }

    public String getTableName() {
        return getClass().getSimpleName().toLowerCase();
    }

    protected abstract SQLiteTable buildTableSchema(SQLiteTable.Builder aBuilder);

    protected abstract Object getValueOfColumn(String aColumnName);

    public void save(Context aContext) {
        ContentValues values = new ContentValues();
        for (int i = 0; i < mTable.getColumns().size(); i++) {
            SQLiteTable.Column column = mTable.getColumns().valueAt(i);
            Object value = getValueOfColumn(column.name);
            if (value == null) {
                continue;
            }

            if (value instanceof Long) {
                values.put(column.name, (Long) value);
            } else if (value instanceof String) {
                values.put(column.name, (String) value);
            } else if (value instanceof Double) {
                values.put(column.name, (Double) value);
            } else if (value instanceof Integer) {
                values.put(column.name, (Integer) value);
            } else if (value instanceof Float) {
                values.put(column.name, (Float) value);
            } else if (value instanceof Boolean) {
                values.put(column.name, (Boolean) value);
            } else if (value instanceof Short) {
                values.put(column.name, (Short) value);
            } else if (value instanceof Byte) {
                values.put(column.name, (Byte) value);
            } else if (value instanceof byte[]) {
                values.put(column.name, (byte[]) value);
            }
        }

        aContext.getContentResolver().insert(mContentUri, values);
    }

    void setTable(SQLiteTable aTable) {
        mTable = aTable;
    }

    void setContentUri(Uri aUri) {
        mContentUri = aUri;
    }
}
