package com.vokal.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.ArrayList;

import static android.text.TextUtils.isEmpty;

public class SQLiteTable {

    public interface TableCreator {
        public SQLiteTable buildTableSchema(Builder aBuilder);
        public SQLiteTable updateTableSchema(Updater aUpdater, int aOldVersion);
    }

    static class Column {
        public int     type;
        public String  name;
        public boolean primary_key;
        public boolean autoincrement;
        public boolean not_null;
        public boolean unique;
        public String  default_value;
    }

    private String            mTableName;
    private ArrayList<Column> mColumns;
    private String[]          mPrimaryKey;
    private String[]          mUnique;
    private String            mCreateSQL;
    private ArrayList<String> mIndicesSQL;
    private ContentValues[]   mSeed;
    private String[]          mUpdateSQL;
    private String            mNullHack;

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

    // TODO: query columns after onCreate/onUpdate since Builder may not have been used to make SQL

    protected ArrayList<Column> getColumns() {
        return mColumns;
    }

    public Column addColumn(int aType, String aName) {
        Column column = new Column();
        column.type = aType;
        column.name = aName;
        mColumns.add(column);
        return column;
    }

    public void addIndex(String aIndexName, String aColumns) {
        if (mIndicesSQL == null) {
            mIndicesSQL = new ArrayList<String>();
        }
        mIndicesSQL.add(String.format("CREATE INDEX %s ON %s (%s);", aIndexName, mTableName, aColumns));
    }

    protected String getCreateSQL() {
        if (isEmpty(mCreateSQL)) {
            ArrayList<String> columnDefs = new ArrayList<String>();

            boolean primaryKeyDefined = false;

            for (Column col : mColumns) {
                String colDef = getColumnDef(col);

                if (col.primary_key) {
                    if (primaryKeyDefined)
                        throw new IllegalStateException("Table '" + mTableName + "' can only have one PRIMARY KEY");

                    colDef = colDef.concat(" PRIMARY KEY");
                    primaryKeyDefined = true;
                    if (col.autoincrement) {
                        colDef = colDef.concat(" AUTOINCREMENT");
                    }
                }

                if (col.not_null) {
                    if (col.type == Cursor.FIELD_TYPE_NULL) {
                        throw new IllegalStateException("Column '" + col.name +
                                                                "' with type NULL cannot have NOT NULL constraint.");
                    }
                    if (col.default_value == null) {
                        throw new IllegalStateException("Column '" + col.name +
                                                                "' NOT NULL constraint requires a default value");
                    }
                    colDef = colDef.concat(" NOT NULL");
                }

                if (col.unique && !col.primary_key) {
                    colDef = colDef.concat(" UNIQUE");
                }

                if (col.default_value != null) {
                    colDef = colDef.concat(String.format(" DEFAULT %s", col.default_value));
                }

                columnDefs.add(colDef);
            }

            if (mPrimaryKey != null) {
                if (primaryKeyDefined)
                    throw new IllegalStateException("table '" + mTableName + "' can only have one PRIMARY KEY");

                columnDefs.add(String.format("PRIMARY KEY (%s)", TextUtils.join(", ", mPrimaryKey)));
            }
            if (mUnique != null) {
                columnDefs.add(String.format("UNIQUE (%s)", TextUtils.join(", ", mUnique)));
            }

            mCreateSQL = String.format("CREATE TABLE %s (%s);", mTableName, TextUtils.join(", ", columnDefs));
        }

        return mCreateSQL;
    }

    protected ArrayList<String> getIndicesSQL() {
        return mIndicesSQL;
    }

    protected ContentValues[] getSeedValues() {
        return mSeed;
    }

    protected String[] getUpdateSQL() {
        if (mUpdateSQL == null) {
            ArrayList<String> columns = new ArrayList<String>();

            for (Column col : mColumns) {
                String colDef = getColumnDef(col);

                if (col.not_null) {
                    if (col.type == Cursor.FIELD_TYPE_NULL) {
                        throw new IllegalStateException("Column '" + col.name +
                                                                "' with type NULL cannot have NOT NULL constraint.");
                    }
                    if (col.default_value == null) {
                        throw new IllegalStateException("Column '" + col.name +
                                                                "' NOT NULL constraint requires a default value");
                    }
                    colDef = colDef.concat(" NOT NULL");
                }

                if (col.default_value != null) {
                    colDef = colDef.concat(String.format(" DEFAULT %s", col.default_value));
                }

                columns.add(colDef);
            }

            mUpdateSQL = new String[columns.size()];
            int index = 0;
            for (String column : columns) {
                mUpdateSQL[index++] = String.format("ALTER TABLE %s ADD COLUMN %s;", mTableName, column);
            }
        }

        return mUpdateSQL;
    }

    protected String getNullHack() {
        return mNullHack;
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

        public Builder(String aTableName) {
            mTable = new SQLiteTable(aTableName);
        }

        //--- Column Adders

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

        private Builder column(String aName, int aType) {
            mLastColumn = mTable.addColumn(aType, aName);
            return this;
        }

        //--- Column Constraints

        public Builder primaryKey() {
            mLastColumn.primary_key = true;
            return this;
        }

        public Builder autoincrement() {
            mLastColumn.autoincrement = true;
            return this;
        }

        public Builder notNull() {
            mLastColumn.not_null = true;
            return this;
        }

        public Builder unique() {
            mLastColumn.unique = true;
            return this;
        }

        public Builder defaultValue(String aString) {
            mLastColumn.default_value = String.format("'%s'", aString);
            return this;
        }

        public Builder defaultValue(long aInteger) {
            mLastColumn.default_value = Long.toString(aInteger);
            return this;
        }

        public Builder defaultValue(double aReal) {
            mLastColumn.default_value = Double.toString(aReal);
            return this;
        }

//        public Builder defaultValue(byte[] aBlob) {
//            mLastColumn.default_value = TODO
//        }

        public Builder defaultCurrentTime() {
            mLastColumn.default_value = "CURRENT_TIME";
            return this;
        }

        public Builder defaultCurrentDate() {
            mLastColumn.default_value = "CURRENT_DATE";
            return this;
        }

        public Builder defaultCurrentTimestamp() {
            mLastColumn.default_value = "CURRENT_TIMESTAMP";
            return this;
        }

        //--- Table Constraints

        public Builder primaryKey(String... aColumns) {
            mTable.mPrimaryKey = aColumns;
            return this;
        }

        public Builder unique(String... aColumns) {
            mTable.mUnique = aColumns;
            return this;
        }

        public Builder seed(ContentValues... aSeed) {
            mTable.mSeed = aSeed;
            return this;
        }

        public Builder nullHack(String aNullHack) {
            mTable.mNullHack = aNullHack;
            return this;
        }

        public Builder index(String... aColumns) {
            String index_name = mTable.mTableName.concat("_")
                    .concat(TextUtils.join("_", aColumns).toLowerCase())
                    .concat("_idx");
            String columns = TextUtils.join(", ", aColumns);
            mTable.addIndex(index_name, columns);
            return this;
        }

        public SQLiteTable build() {
            return mTable;
        }
    }

    public static class Updater {

        private final SQLiteTable mTable;
        private Column mLastColumn;

        public Updater(String aTableName) {
            mTable = new SQLiteTable(aTableName);
        }

        public Updater addStringColumn(String name) {
            return column(name, Cursor.FIELD_TYPE_STRING);
        }

        public Updater addIntegerColumn(String name) {
            return column(name, Cursor.FIELD_TYPE_INTEGER);
        }

        public Updater addRealColumn(String name) {
            return column(name, Cursor.FIELD_TYPE_FLOAT);
        }

        public Updater addBlobColumn(String name) {
            return column(name, Cursor.FIELD_TYPE_BLOB);
        }

        public Updater addNullColumn(String name) {
            return column(name, Cursor.FIELD_TYPE_NULL);
        }

        private Updater column(String aName, int aType) {
            mLastColumn = mTable.addColumn(aType, aName);
            return this;
        }

        public Updater notNull() {
            if (mLastColumn.type == Cursor.FIELD_TYPE_NULL) {
                throw new IllegalStateException("Column '" + mLastColumn.name +
                                                        "' with type NULL cannot have NOT NULL constraint)");
            }
            mLastColumn.not_null = true;
            return this;
        }

        public Updater defaultValue(String aString) {
            mLastColumn.default_value = String.format("'%s'", aString);
            return this;
        }

        public Updater defaultValue(long aInteger) {
            mLastColumn.default_value = Long.toString(aInteger);
            return this;
        }

        public Updater defaultValue(double aReal) {
            mLastColumn.default_value = Double.toString(aReal);
            return this;
        }

        public Updater nullHack(String aNullHack) {
            mTable.mNullHack = aNullHack;
            return this;
        }

        public Updater seed(ContentValues... aSeed) {
            mTable.mSeed = aSeed;
            return this;
        }

        public Updater index(String... aColumns) {
            String index_name = mTable.mTableName.concat("_")
                    .concat(TextUtils.join("_", aColumns).toLowerCase())
                    .concat("_idx");
            String columns = TextUtils.join(", ", aColumns);
            mTable.addIndex(index_name, columns);
            return this;
        }

        public SQLiteTable build() {
            return mTable;
        }
    }

}

