package com.vokal.database;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

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

    private String            mTableName;
    private ArrayList<Column> mColumns;
    private String[]          mPrimaryKey;
    private String[]          mUnique;
    private String[]          mIndex;

    private String mCreateSQL;
    private String mUpdateSQL;

    public SQLiteTable(String aTableName) {
        mTableName = aTableName;
        mColumns = new ArrayList<Column>();
    }

    public void setTableName(String aTableName) {
        mTableName = aTableName;
    }

    public String getTableName() {
        return mTableName;
    }

    protected ArrayList<Column> getColumns() {
        return mColumns;
    }

    protected String getCreateSQL() {
        if (isEmpty(mCreateSQL)) {
            ArrayList<String> columnDefs = new ArrayList<String>();

            for (Column col : mColumns) {
                String colDef = getColumnDef(col);

                if (col.primary_key) {
                    colDef = colDef.concat(" PRIMARY KEY");
                    if (col.autoincrement) {
                        colDef = colDef.concat(" AUTOINCREMENT");
                    }
                    columnDefs.add(colDef);
                    continue; // single primary key is inherently unique
                }

                if (col.unique) {
                    colDef = colDef.concat(" UNIQUE");
                }
                columnDefs.add(colDef);
            }

            if (mPrimaryKey != null) {
                columnDefs.add(String.format("PRIMARY KEY (%s)", TextUtils.join(", ", mPrimaryKey)));
            }
            if (mUnique != null) {
                columnDefs.add(String.format("UNIQUE (%s)", TextUtils.join(", ", mUnique)));
            }
            if (mIndex != null) {
                columnDefs.add(String.format("INDEX (%s)", TextUtils.join(", ", mIndex)));
            }

            mCreateSQL = String.format("CREATE TABLE %s (%s);", mTableName, TextUtils.join(", ", columnDefs));
        }

        Log.d(TAG, "create SQL: " + mCreateSQL);
        return mCreateSQL;
    }

    protected String getUpdateSQL() {
        // TODO:
        return null;
    }

    protected String getColumnDef(Column column) {
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

        public Builder primaryKey(String... aColumns) {
            mTable.mPrimaryKey = aColumns;
            return this;
        }

        public Builder unique(String... aColumns) {
            mTable.mUnique = aColumns;
            return this;
        }

        public Builder index(String... aColumns) {
            mTable.mIndex = aColumns;
            return this;
        }

        public SQLiteTable build() {
            return mTable;
        }

        private Builder column(String aName, int aType) {
            mLastColumn = new Column();
            mLastColumn.name = aName;
            mLastColumn.type = aType;

            mTable.mColumns.add(mLastColumn);
            return this;
        }

    }
}

