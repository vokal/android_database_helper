package com.vokal.database;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.provider.BaseColumns;
import android.net.Uri;

public abstract class AbstractDataModel implements BaseColumns {

    public static final long UNDEFINED = -1;
    public static final String SELECT_BY_ID = _ID + " = ?";

    protected Long _id = UNDEFINED;

    public final Uri getContentUri() {
        return DatabaseHelper.getContentUri(getClass());
    }

    private boolean hasId() {
        return _id != null && _id != UNDEFINED;
    }

    public Uri save(Context aContext) {
        ContentValues values = new ContentValues();
        populateContentValues(values);

        Uri uri = Uri.EMPTY;
        if (hasId()) {
            uri = aContext.getContentResolver().insert(getContentUri(), values);
            try {
                _id = ContentUris.parseId(uri);
            } catch (Exception e) {
            }
        } else {
            uri = ContentUris.withAppendedId(getContentUri(), _id);
            aContext.getContentResolver().update(uri, values, null, null);
        }

        return uri;
    }

    public boolean delete(Context aContext) {
        boolean result = false;
        if (hasId()) {
            result = (aContext.getContentResolver().delete(getContentUri(),
                    SELECT_BY_ID, new String[]{String.valueOf(_id)}) == 1);
        }
        return result;
    }

    protected void populateContentValues(ContentValues aValues) {
        if (hasId()) aValues.put(_ID, _id);
    }


}
