package com.vokal.db.test.models;

import java.util.Date;

public class BaseModel {

    private String  mString;
    private Date    mDate;
    private boolean mBoolean;
    private int     mInteger;
    private long    mLong;
    private float   mFloat;
    private double  mDouble;

    public String getString() {
        return mString;
    }

    public void setString(String aString) {
        mString = aString;
    }

    public boolean isBoolean() {
        return mBoolean;
    }

    public void setBoolean(boolean aBoolean) {
        mBoolean = aBoolean;
    }

    public int getInteger() {
        return mInteger;
    }

    public void setInteger(int aInteger) {
        mInteger = aInteger;
    }

    public long getLong() {
        return mLong;
    }

    public void setLong(long aLong) {
        mLong = aLong;
    }

    public float getFloat() {
        return mFloat;
    }

    public void setFloat(float aFloat) {
        mFloat = aFloat;
    }

    public double getDouble() {
        return mDouble;
    }

    public void setDouble(double aDouble) {
        mDouble = aDouble;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date aDate) {
        mDate = aDate;
    }
}
