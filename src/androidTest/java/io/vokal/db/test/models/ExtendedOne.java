package io.vokal.db.test.models;


import android.content.ContentValues;

import io.vokal.db.AbstractDataModel;
import io.vokal.db.SQLiteTable;
import io.vokal.db.util.CursorCreator;
import io.vokal.db.util.CursorGetter;

import java.lang.String;

public class ExtendedOne extends AbstractDataModel {

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
        public SQLiteTable updateTableSchema(SQLiteTable.Upgrader aUpgrader, int aOldVersion) {
            return null;
        }
    };

    public static final CursorCreator<ExtendedOne> CURSOR_CREATOR = new CursorCreator<ExtendedOne>() {
        public ExtendedOne createFromCursorGetter(CursorGetter getter) {

            return new ExtendedOne(getter);
        }
    };

    public static final String COL_STRING  = "string1";
    public static final String COL_BOOLEAN = "boolean1";
    public static final String COL_INT     = "int1";
    public static final String COL_LONG    = "long1";
    public static final String COL_DOUBLE  = "double1";

    private String  string1;
    private boolean boolean1;
    private int     int1;
    private long    long1;
    private double  double1;

    public ExtendedOne() {}

    protected ExtendedOne(CursorGetter getter) {
        super(getter);
        string1 = getter.getString(COL_STRING);
        boolean1 = getter.getBoolean(COL_BOOLEAN);
        int1 = getter.getInt(COL_INT);
        long1 = getter.getLong(COL_LONG);
        double1 = getter.getDouble(COL_DOUBLE);
    }

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
    }
}
