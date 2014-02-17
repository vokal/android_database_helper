package com.vokal.database;

import android.net.Uri;

public abstract class AbstractDataModel {

    private Uri mContentUri;
    private SQLiteTable mTable;  // TODO: guarantee that mTable.mColumns is still created if Builder is not used

    public AbstractDataModel() {}

    public final Uri getContentUri() {
        return mContentUri;
    }

    protected abstract SQLiteTable buildTableSchema(SQLiteTable.Builder aBuilder);

    protected abstract SQLiteTable updateTableSchema(SQLiteTable.Builder aBuilder, int aOldVersion);

//    protected abstract Object getValueOfColumn(String aColumnName);

    // TODO: query columns after onCreate/onUpdate since Builder may not have been used to make SQL
//    public void save(Context aContext) {
//        ContentValues values = new ContentValues();
//        for (SQLiteTable.Column column : mTable.getColumns()) {
//            Object value = getValueOfColumn(column.name);
//            if (value == null) {
//                continue;
//            }
//
//            if (value instanceof Long) {
//                values.put(column.name, (Long) value);
//            } else if (value instanceof String) {
//                values.put(column.name, (String) value);
//            } else if (value instanceof Double) {
//                values.put(column.name, (Double) value);
//            } else if (value instanceof Integer) {
//                values.put(column.name, (Integer) value);
//            } else if (value instanceof Float) {
//                values.put(column.name, (Float) value);
//            } else if (value instanceof Boolean) {
//                values.put(column.name, (Boolean) value);
//            } else if (value instanceof Short) {
//                values.put(column.name, (Short) value);
//            } else if (value instanceof Byte) {
//                values.put(column.name, (Byte) value);
//            } else if (value instanceof byte[]) {
//                values.put(column.name, (byte[]) value);
//            }
//        }
//
//        aContext.getContentResolver().insert(mContentUri, values);
//    }

    void setContentUri(Uri aUri) {
        mContentUri = aUri;
    }

    void setTable(SQLiteTable aTable) {
        mTable = aTable;
    }
}
