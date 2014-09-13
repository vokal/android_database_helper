package com.vokal.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Locale;

import static android.text.TextUtils.isEmpty;

public class SQLiteTable {

    static final int FIELD_TYPE_NULL    = 0;
    static final int FIELD_TYPE_INTEGER = 1;
    static final int FIELD_TYPE_FLOAT   = 2;
    static final int FIELD_TYPE_STRING  = 3;
    static final int FIELD_TYPE_BLOB    = 4;

    public interface TableCreator {
        public @Nullable SQLiteTable buildTableSchema(Builder aBuilder);
        public @Nullable SQLiteTable updateTableSchema(Upgrader aUpgrader, int aOldVersion);
    }

    public static class Column {
        public int     type;
        public String  name;
        public boolean primary_key;
        public boolean autoincrement;
        public boolean not_null;
        public boolean unique;
        public String  default_value;

        Column(String aName, int aType) {
            name = aName;
            type = aType;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Column)) return false;

            Column other = (Column) o;
            return other.name.equals(name);
        }
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

    private boolean           mRecreateOnUpgrade;

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
        Column column = new Column(aName, aType);
        if (!mColumns.contains(column))
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
                    primaryKeyDefined = true;

                    colDef = colDef.concat(" PRIMARY KEY");
                    if (col.autoincrement) {
                        colDef = colDef.concat(" AUTOINCREMENT");
                    }
                }

                if (col.not_null) {
                    if (col.type == FIELD_TYPE_NULL) {
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
                primaryKeyDefined = true;

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
                    if (col.type == FIELD_TYPE_NULL) {
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
            case FIELD_TYPE_INTEGER: type = "INTEGER"; break;
            case FIELD_TYPE_FLOAT: type = "REAL"; break;
            case FIELD_TYPE_STRING: type = "TEXT"; break;
            case FIELD_TYPE_BLOB: type = "BLOB"; break;
        }
        return column.name.concat(" ").concat(type);
    }

    protected boolean isCleanUpgrade() {
        return mRecreateOnUpgrade;
    }

    public static class Builder {

        private final SQLiteTable mTable;
        private Column mLastColumn;
        private boolean androidIdDefined;
        private boolean primaryKeyDefined;

        public Builder(String aTableName) {
            mTable = new SQLiteTable(aTableName);
        }

        //--- Column Adders

        public Builder addStringColumn(String name) {
            return column(name, FIELD_TYPE_STRING);
        }

        public Builder addIntegerColumn(String name) {
            if (BaseColumns._ID.equals(name))
                androidIdDefined = true;
            return column(name, FIELD_TYPE_INTEGER);
        }

        public Builder addRealColumn(String name) {
            return column(name, FIELD_TYPE_FLOAT);
        }

        public Builder addBlobColumn(String name) {
            return column(name, FIELD_TYPE_BLOB);
        }

        public Builder addNullColumn(String name) {
            return column(name, FIELD_TYPE_NULL);
        }

        private Builder column(String aName, int aType) {
            mLastColumn = mTable.addColumn(aType, aName);
            return this;
        }

        private void checkColumn(String aCaller) {
            if (mLastColumn == null) {
                throw new IllegalStateException("You can only call '" + aCaller + "' after adding a column!");
            }
        }

        //--- Column Constraints

        public Builder primaryKey() {
            checkColumn("primaryKey()");

            if (primaryKeyDefined)
                throw new IllegalStateException("Table '" + mTable.mTableName + "' can only have one PRIMARY KEY");
            primaryKeyDefined = true;

            mLastColumn.primary_key = true;
            return this;
        }

        public Builder autoincrement() {
            checkColumn("autoincrement()");
            mLastColumn.autoincrement = true;
            return this;
        }

        public Builder notNull() {
            checkColumn("notNull()");
            mLastColumn.not_null = true;
            return this;
        }

        public Builder unique() {
            checkColumn("unique()");
            mLastColumn.unique = true;
            return this;
        }

        public Builder defaultValue(String aString) {
            checkColumn("defaultValue()");
            mLastColumn.default_value = String.format("'%s'", aString);
            return this;
        }

        public Builder defaultValue(long aInteger) {
            checkColumn("defaultValue()");
            mLastColumn.default_value = Long.toString(aInteger);
            return this;
        }

        public Builder defaultValue(double aReal) {
            checkColumn("defaultValue()");
            mLastColumn.default_value = Double.toString(aReal);
            return this;
        }

//        public Builder defaultValue(byte[] aBlob) {
//            mLastColumn.default_value = TODO
//        }

        public Builder defaultCurrentTime() {
            checkColumn("defaultCurrentTime()");
            mLastColumn.default_value = "CURRENT_TIME";
            return this;
        }

        public Builder defaultCurrentDate() {
            checkColumn("defaultCurrentDate()");
            mLastColumn.default_value = "CURRENT_DATE";
            return this;
        }

        public Builder defaultCurrentTimestamp() {
            checkColumn("defaultCurrentTimestamp()");
            mLastColumn.default_value = "CURRENT_TIMESTAMP";
            return this;
        }

        //--- Table Constraints

        public Builder primaryKey(String... aColumns) {
            if (primaryKeyDefined)
                throw new IllegalStateException("Table '" + mTable.mTableName + "' can only have one PRIMARY KEY");
            primaryKeyDefined = true;

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
                    .concat(TextUtils.join("_", aColumns).toLowerCase(Locale.getDefault()))
                    .concat("_idx");
            String columns = TextUtils.join(", ", aColumns);
            mTable.addIndex(index_name, columns);
            return this;
        }

        public SQLiteTable build() {
            if (!androidIdDefined) {
                Column col = new Column(BaseColumns._ID, FIELD_TYPE_INTEGER);
                col.autoincrement = true;
                if (!primaryKeyDefined) {
                    col.primary_key = true;
                }
                mTable.mColumns.add(col);
            }

            return mTable;
        }
    }

    public static class Upgrader {

        private final SQLiteTable mTable;
        private Column mLastColumn;

        public Upgrader(String aTableName) {
            mTable = new SQLiteTable(aTableName);
        }

        public Upgrader addStringColumn(String name) {
            return column(name, FIELD_TYPE_STRING);
        }

        public Upgrader addIntegerColumn(String name) {
            return column(name, FIELD_TYPE_INTEGER);
        }

        public Upgrader addRealColumn(String name) {
            return column(name, FIELD_TYPE_FLOAT);
        }

        public Upgrader addBlobColumn(String name) {
            return column(name, FIELD_TYPE_BLOB);
        }

        public Upgrader addNullColumn(String name) {
            return column(name, FIELD_TYPE_NULL);
        }

        private Upgrader column(String aName, int aType) {
            mLastColumn = mTable.addColumn(aType, aName);
            return this;
        }

        private void checkColumn(String aCaller) {
            if (mLastColumn == null) {
                throw new IllegalStateException("You must call '" + aCaller + "' after adding a column!");
            }
        }

        public Upgrader notNull() {
            checkColumn("notNull()");
            if (mLastColumn.type == Cursor.FIELD_TYPE_NULL) {
                throw new IllegalStateException("Column '" + mLastColumn.name +
                                                        "' with type NULL cannot have NOT NULL constraint)");
            }
            mLastColumn.not_null = true;
            return this;
        }

        public Upgrader defaultValue(String aString) {
            checkColumn("defaultValue(String)");
            mLastColumn.default_value = String.format("'%s'", aString);
            return this;
        }

        public Upgrader defaultValue(long aInteger) {
            checkColumn("defaultValue(long)");
            mLastColumn.default_value = Long.toString(aInteger);
            return this;
        }

        public Upgrader defaultValue(double aReal) {
            checkColumn("defaultValue(double)");
            mLastColumn.default_value = Double.toString(aReal);
            return this;
        }

        public Upgrader nullHack(String aNullHack) {
            mTable.mNullHack = aNullHack;
            return this;
        }

        public Upgrader seed(ContentValues... aSeed) {
            mTable.mSeed = aSeed;
            return this;
        }

        public Upgrader index(String... aColumns) {
            String index_name = mTable.mTableName.concat("_")
                    .concat(TextUtils.join("_", aColumns).toLowerCase(Locale.getDefault()))
                    .concat("_idx");
            String columns = TextUtils.join(", ", aColumns);
            mTable.addIndex(index_name, columns);
            return this;
        }

        public Upgrader recreate() {
            mTable.mRecreateOnUpgrade = true;
            return this;
        }

        public SQLiteTable build() {
            return mTable;
        }
    }

    static boolean isTableAbstractDataModel(String aTableName) {
        Class clazz = DatabaseHelper.CLASS_MAP.get(aTableName);
        return clazz != null && AbstractDataModel.class.isAssignableFrom(clazz);
    }

    static boolean isTableDataModelInterface(String aTableName) {
        Class clazz = DatabaseHelper.CLASS_MAP.get(aTableName);
        return clazz != null && DataModelInterface.class.isAssignableFrom(clazz);
    }

    static TableCreator getDefaultCreator(final AbstractDataModel aModel) {
        return new TableCreator() {
            @Override
            public SQLiteTable buildTableSchema(Builder aBuilder) {
                return aModel.onTableCreate(aBuilder);
            }

            @Override
            public SQLiteTable updateTableSchema(Upgrader aUpgrader, int aOldVersion) {
                return aModel.onTableUpgrade(aUpgrader, aOldVersion);
            }
        };
    }

    static TableCreator getDefaultCreator(final DataModelInterface aModel) {
        return new TableCreator() {
            @Override
            public SQLiteTable buildTableSchema(Builder aBuilder) {
                return aModel.onTableCreate(aBuilder);
            }

            @Override
            public SQLiteTable updateTableSchema(Upgrader aUpgrader, int aOldVersion) {
                return aModel.onTableUpgrade(aUpgrader, aOldVersion);
                // TODO: auto-recreate if null and schema has changed
            }
        };
    }

}

