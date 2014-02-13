package com.vokal.database;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;

import static android.text.TextUtils.isEmpty;

public class SQLiteTable {

    private static final String TAG = SQLiteTable.class.getSimpleName();

    static class Column {

        public int     type;
        public String  name;
        public boolean primary_key;
        public boolean autoincrement;
        public boolean unique;
    }

    private String              mTableName;
    private SparseArray<Column> mColumns;

    private String mCreateSQL;

    public SQLiteTable(String aTableName) {
        mTableName = aTableName;
        mColumns = new SparseArray<Column>();
    }

    public void setTableName(String aTableName) {
        mTableName = aTableName;
    }

    public String getTableName() {
        return mTableName;
    }

    SparseArray<Column> getColumns() {
        return mColumns;
    }

    String getCreateSQL() {
        if (isEmpty(mCreateSQL)) {
            ArrayList<Column> keys = new ArrayList<Column>();
            ArrayList<Column> unique = new ArrayList<Column>();

            // accumulate keys & unique
            for (int i = 0; i < mColumns.size(); i++) {
                Column col = mColumns.valueAt(i);
                if (col.primary_key) {
                    keys.add(col);
                }
                if (col.unique) {
                    unique.add(col);
                }
            }

            Log.d(TAG, String.format("building SQL for `%s`: columns=%d, keys=%d, unique=%d", mTableName,
                                     mColumns.size(), keys.size(), unique.size()));

            ArrayList<String> columnDefs = new ArrayList<String>();


            for (int i = 0; i < mColumns.size(); i++) {
                Column col = mColumns.valueAt(i);
                String colDef = getColumnDef(col);

                if (col.primary_key && keys.size() == 1) {
                    colDef = colDef.concat(" PRIMARY KEY");
                    if (col.autoincrement) {
                        colDef = colDef.concat(" AUTOINCREMENT");
                    }
                    columnDefs.add(colDef);
                    continue; // single primary key is inherently unique
                }

                if (col.unique && unique.size() == 1) {
                    colDef = colDef.concat(" UNIQUE");
                }

                columnDefs.add(colDef);
            }
            if (keys.size() > 1) {
                ArrayList<String> keyNames = new ArrayList<String>();
                for (Column column : keys) {
                    keyNames.add(column.name);
                }
                columnDefs.add(" PRIMARY KEY (".concat(TextUtils.join(", ", keyNames).concat(")")));
            }
            if (unique.size() > 1) {
                ArrayList<String> uniqueNames = new ArrayList<String>();
                for (Column column : unique) {
                    uniqueNames.add(column.name);
                }
                columnDefs.add(" UNIQUE (".concat(TextUtils.join(", ", uniqueNames).concat(")")));
            }

            mCreateSQL = "CREATE TABLE " + mTableName + " (" + TextUtils.join(", ", columnDefs) + ");";
        }

        Log.d(TAG, "create SQL: " + mCreateSQL);
        return mCreateSQL;
    }

    private String getColumnDef(Column column) {
        String type = "NULL";
        switch (column.type) {
            case Cursor.FIELD_TYPE_INTEGER: type = "INTEGER"; break;
            case Cursor.FIELD_TYPE_FLOAT: type = "REAL"; break;
            case Cursor.FIELD_TYPE_STRING: type = "TEXT"; break;
            case Cursor.FIELD_TYPE_BLOB: type = "BLOB"; break;
        }
        return column.name.concat(" ").concat(type);
    }

    public static class Builder {

        private final SQLiteTable mTable;
        private Column mLastColumn;
        private int mNextIndex = 0;

        public Builder(String aTableName) {
            mTable = new SQLiteTable(aTableName);
        }

        public Builder addStringColumn(String name) {
            return column(name, Cursor.FIELD_TYPE_STRING);
        }

        public Builder addIntegerColumn(String name) {
            return column(name, Cursor.FIELD_TYPE_INTEGER);
        }

        public Builder addRealColumn(String name) {
            return column(name, Cursor.FIELD_TYPE_FLOAT);
        }

        public Builder addBlobColumn(String name) {
            return column(name, Cursor.FIELD_TYPE_BLOB);
        }

        public Builder addNullColumn(String name) {
            return column(name, Cursor.FIELD_TYPE_NULL);
        }

        public Builder primaryKey() {
            mLastColumn.primary_key = true;
            return this;
        }

        public Builder autoincrement() {
            mLastColumn.autoincrement = true;
            return this;
        }

        public Builder unique() {
            mLastColumn.unique = true;
            return this;
        }

        public SQLiteTable build() {
            return mTable;
        }

        private Builder column(String aName, int aType) {
            mLastColumn = new Column();
            mLastColumn.name = aName;
            mLastColumn.type = aType;

            mTable.mColumns.append(mNextIndex++, mLastColumn);
            return this;
        }

    }
}

