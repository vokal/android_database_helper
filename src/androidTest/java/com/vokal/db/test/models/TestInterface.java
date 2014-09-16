package com.vokal.db.test.models;

import android.content.ContentValues;

import com.vokal.db.DataModelInterface;
import com.vokal.db.SQLiteTable;
import com.vokal.db.util.CursorCreator;
import com.vokal.db.util.CursorGetter;

public class TestInterface extends BaseModel implements DataModelInterface {

    public static int sDBVersion = 1;

    public static final String COL_STRING  = "string";
    public static final String COL_BOOLEAN = "boolean";
    public static final String COL_INT     = "int";
    public static final String COL_LONG    = "long";
    public static final String COL_FLOAT   = "float";
    public static final String COL_DOUBLE  = "double";
    public static final String COL_DATE    = "date";

    public static final String COL_STRING_NEW  = "string_new";

    public static final CursorCreator<TestInterface> CURSOR_CREATOR = new CursorCreator<TestInterface>() {
        @Override
        public TestInterface createFromCursorGetter(CursorGetter getter) {
            return new TestInterface(getter);
        }
    };

    public String newString;

    public TestInterface() {}

    public TestInterface(CursorGetter getter) {
        setString(getter.getString(COL_STRING));
        setBoolean(getter.getBoolean(COL_BOOLEAN));
        setInteger(getter.getInt(COL_INT));
        setLong(getter.getLong(COL_LONG));
        setFloat(getter.getFloat(COL_FLOAT));
        setDouble(getter.getDouble(COL_DOUBLE));
        setDate(getter.getDate(COL_DATE));

        if (sDBVersion > 1)
        newString = getter.getString(COL_STRING_NEW);
    }

    @Override
    public SQLiteTable onTableCreate(SQLiteTable.Builder aBuilder) {
        return aBuilder.addStringColumn(COL_STRING)
                .addIntegerColumn(COL_BOOLEAN)
                .addIntegerColumn(COL_INT)
                .addIntegerColumn(COL_LONG).primaryKey()
                .addRealColumn(COL_FLOAT)
                .addRealColumn(COL_DOUBLE)
                .addIntegerColumn(COL_DATE)
                .build();
    }

    @Override
    public SQLiteTable onTableUpgrade(SQLiteTable.Upgrader aUpgrader, int aOldVersion) {
        return aUpgrader.addStringColumn(COL_STRING_NEW).build();
    }

    @Override
    public void populateContentValues(ContentValues aValues) {
        aValues.put(COL_STRING, getString());
        aValues.put(COL_BOOLEAN, isBoolean());
        aValues.put(COL_INT, getInteger());
        aValues.put(COL_LONG, getLong());
        aValues.put(COL_FLOAT, getFloat());
        aValues.put(COL_DOUBLE, getDouble());
        aValues.put(COL_DATE, getDate().getTime());

        if (sDBVersion > 1)
            aValues.put(COL_STRING_NEW, newString);
    }

}
