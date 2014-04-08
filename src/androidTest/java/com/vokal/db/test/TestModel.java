package com.vokal.db.test;


import android.content.ContentValues;

import com.vokal.db.AbstractDataModel;
import com.vokal.db.SQLiteTable;

import java.lang.String;

public class TestModel extends AbstractDataModel {

    public static final SQLiteTable.TableCreator TABLE_CREATOR = new SQLiteTable.TableCreator() {

        @Override
        public SQLiteTable buildTableSchema(SQLiteTable.Builder aBuilder) {

            aBuilder.addStringColumn(COL_STRING)
                    .addIntegerColumn(COL_BOOLEAN)
                    .addIntegerColumn(COL_LONG)
                    .addIntegerColumn(COL_INT).unique().autoincrement()
                    .addIntegerColumn(COL_DOUBLE);

            return aBuilder.build();
        }

        @Override
        public SQLiteTable updateTableSchema(SQLiteTable.Updater aUpdater, int aOldVersion) {
            return null;
        }
    };

    public static final String COL_STRING = "string1";
    public static final String COL_BOOLEAN = "boolean1";
    public static final String COL_INT = "int1";
    public static final String COL_LONG = "long1";
    public static final String COL_DOUBLE = "double1";

    private String string1;
    private boolean boolean1;
    private int int1;
    private long long1;
    private double double1;

    public long getId() {
        return _id;
    }

    public void setId(long id) {
        this._id = id;
    }

    public String getString1() {
        return string1;
    }

    public void setString1(String string1) {
        this.string1 = string1;
    }

    public boolean isBoolean1() {
        return boolean1;
    }

    public void setBoolean1(boolean boolean1) {
        this.boolean1 = boolean1;
    }

    public int getInt1() {
        return int1;
    }

    public void setInt1(int int1) {
        this.int1 = int1;
    }

    public long getLong1() {
        return long1;
    }

    public void setLong1(long long1) {
        this.long1 = long1;
    }

    public double getDouble1() {
        return double1;
    }

    public void setDouble1(double double1) {
        this.double1 = double1;
    }


    @Override
    public void populateContentValues(ContentValues aValues) {
        aValues.put(COL_STRING, string1);
        aValues.put(COL_BOOLEAN, boolean1);
        aValues.put(COL_INT, int1);
        aValues.put(COL_LONG, long1);
        aValues.put(COL_DOUBLE, double1);
        super.populateContentValues(aValues);
    }
}
