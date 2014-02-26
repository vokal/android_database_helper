package com.vokal.database;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.os.Build;

public class CursorGetter {

    private final Cursor mCursor;

    public CursorGetter(Cursor aCursor) {
        mCursor = aCursor;
    }

    public String getString(String aColumn) {
        return mCursor.getString(mCursor.getColumnIndex(aColumn));
    }

    public short getShort(String aColumn) {
        return mCursor.getShort(mCursor.getColumnIndex(aColumn));
    }

    public int getInt(String aColumn) {
        return mCursor.getInt(mCursor.getColumnIndex(aColumn));
    }

    public long getLong(String aColumn) {
        return mCursor.getLong(mCursor.getColumnIndex(aColumn));
    }

    public float getFloat(String aColumn) {
        return mCursor.getFloat(mCursor.getColumnIndex(aColumn));
    }

    public double getDouble(String aColumn) {
        return mCursor.getDouble(mCursor.getColumnIndex(aColumn));
    }

    public byte[] getBlob(String aColumn) {
        return mCursor.getBlob(mCursor.getColumnIndex(aColumn));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private int getType(String aColumn) {
        return mCursor.getType(mCursor.getColumnIndex(aColumn));
    }

}
