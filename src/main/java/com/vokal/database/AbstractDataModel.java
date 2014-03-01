package com.vokal.database;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

public abstract class AbstractDataModel {

    public AbstractDataModel() {}

    public final Uri getContentUri() {
        return DatabaseHelper.getContentUri(getClass());
    }

    public Uri save(Context aContext) {
        ContentValues values = new ContentValues();
        populateContentValues(values);

        return aContext.getContentResolver().insert(getContentUri(), values);
    }

    protected abstract void populateContentValues(ContentValues aValues);


}
