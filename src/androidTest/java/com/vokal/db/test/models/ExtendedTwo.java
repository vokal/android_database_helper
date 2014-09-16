package com.vokal.db.test.models;


import android.content.ContentValues;
import android.os.Parcel;

import java.util.Date;

import com.vokal.db.AbstractDataModel;
import com.vokal.db.SQLiteTable;
import com.vokal.db.util.CursorCreator;
import com.vokal.db.util.CursorGetter;

public class ExtendedTwo extends AbstractDataModel {

    public static final String COL_DATE    = "date1";
    public static final String COL_STRING  = "string1";
    public static final String COL_BOOLEAN = "boolean1";
    public static final String COL_INT     = "int1";
    public static final String COL_LONG    = "long1";
    public static final String COL_FLOAT   = "float1";
    public static final String COL_DOUBLE  = "double1";

    public static final SQLiteTable.TableCreator TABLE_CREATOR = new SQLiteTable.TableCreator() {

        @Override
        public SQLiteTable buildTableSchema(SQLiteTable.Builder aBuilder) {

            aBuilder.addIntegerColumn(COL_DATE)
                    .addStringColumn(COL_STRING)
                    .addIntegerColumn(COL_BOOLEAN)
                    .addIntegerColumn(COL_INT).unique().autoincrement()
                    .addIntegerColumn(COL_LONG)
                    .addIntegerColumn(COL_FLOAT)
                    .addIntegerColumn(COL_DOUBLE);

            return aBuilder.build();
        }

        @Override
        public SQLiteTable updateTableSchema(SQLiteTable.Upgrader aUpgrader, int aOldVersion) {
            return null;
        }
    };

    public static final CursorCreator<ExtendedTwo> CURSOR_CREATOR = new CursorCreator<ExtendedTwo>() {
        public ExtendedTwo createFromCursorGetter(CursorGetter getter) {
            ExtendedTwo model = new ExtendedTwo();
            model.date1 = getter.getDate(COL_DATE);
            model.string1 = getter.getString(COL_STRING);
            model.boolean1 = getter.getBoolean(COL_BOOLEAN);
            model.int1 = getter.getInt(COL_INT);
            model.long1 = getter.getLong(COL_LONG);
            model.float1 = getter.getFloat(COL_FLOAT);
            model.double1 = getter.getDouble(COL_DOUBLE);
            model._id = getter.getLong("_id");

            return model;
        }
    };

    private Date    date1;
    private String  string1;
    private boolean boolean1;
    private int     int1;
    private long    long1;
    private float   float1;
    private double  double1;

    public ExtendedTwo() {}

    public ExtendedTwo(Parcel aSource) {
        super(aSource);
        long date = aSource.readLong();
        date1 = date == -1 ? null : new Date(date);
        string1 = aSource.readString();
        boolean1 = aSource.readInt() == 1;
        int1 = aSource.readInt();
        long1 = aSource.readLong();
        float1 = aSource.readFloat();
        double1 = aSource.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(date1 == null ? -1 : date1.getTime());
        dest.writeString(string1);
        dest.writeInt(boolean1 ? 1 : 0);
        dest.writeInt(int1);
        dest.writeLong(long1);
        dest.writeFloat(float1);
        dest.writeDouble(double1);
    }

    public long getId() {
        return _id;
    }

    public void setId(long id) {
        this._id = id;
    }

    public Date getDate1() {
        return date1;
    }

    public void setDate1(Date date1) {
        this.date1 = date1;
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

    public float getFloat1() {
        return float1;
    }

    public void setFloat1(float float1) {
        this.float1 = float1;
    }


    public double getDouble1() {
        return double1;
    }

    public void setDouble1(double double1) {
        this.double1 = double1;
    }

    @Override
    public void populateContentValues(ContentValues aValues) {
        if (date1 != null) aValues.put(COL_DATE, date1.getTime());

        aValues.put(COL_STRING, string1);
        aValues.put(COL_BOOLEAN, boolean1);
        aValues.put(COL_INT, int1);
        aValues.put(COL_LONG, long1);
        aValues.put(COL_FLOAT, float1);
        aValues.put(COL_DOUBLE, double1);

        super.populateContentValues(aValues);
    }
}
