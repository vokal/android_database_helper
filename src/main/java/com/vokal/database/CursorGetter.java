package com.vokal.database;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.os.Build;

import static android.text.TextUtils.isEmpty;

public class CursorGetter {

    private final Cursor mCursor;
    private String mTable;

    public CursorGetter(Cursor aCursor) {
        this(aCursor, null);
    }

    public CursorGetter(Cursor aCursor, String aTableName) {
        mCursor = aCursor;
        setTable(aTableName);
    }

    public void setTable(String aTableName) {
        if (isEmpty(aTableName)) {
            mTable = null;
        } else {
            mTable = aTableName.concat(".");
        }
    }

    public String getString(String aColumn) {
        return mCursor.getString(getColumnIndex(aColumn));
    }

    public short getShort(String aColumn) {
        return mCursor.getShort(getColumnIndex(aColumn));
    }

    public int getInt(String aColumn) {
        return mCursor.getInt(getColumnIndex(aColumn));
    }

    public long getLong(String aColumn) {
        return mCursor.getLong(getColumnIndex(aColumn));
    }

    public float getFloat(String aColumn) {
        return mCursor.getFloat(getColumnIndex(aColumn));
    }

    public double getDouble(String aColumn) {
        return mCursor.getDouble(getColumnIndex(aColumn));
    }

    public byte[] getBlob(String aColumn) {
        return mCursor.getBlob(getColumnIndex(aColumn));
    }

    private int getColumnIndex(String aColumn) {
        if (mTable != null) {
            return mCursor.getColumnIndex(mTable.concat(aColumn));
        }
        return mCursor.getColumnIndex(aColumn);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private int getType(String aColumn) {
        return mCursor.getType(mCursor.getColumnIndex(aColumn));
    }

}
