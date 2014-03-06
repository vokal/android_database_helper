package com.vokal.database;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.net.Uri;

import java.util.List;

import timber.log.Timber;

public abstract class AbstractDataModel implements BaseColumns, Parcelable {

    public static final long   UNDEFINED    = -1;
    public static final String SELECT_BY_ID = _ID + " = ?";

    protected transient Long _id = UNDEFINED;

    protected AbstractDataModel() {}

    protected AbstractDataModel(Parcel aSource) {
        _id = aSource.readLong();
    }

    protected AbstractDataModel(CursorGetter aGetter) {
        _id = aGetter.getLong(_ID);
    }

    public long getId() {
        return _id;
    }

    public String getIdString() {
        return Long.toString(_id);
    }

    public String[] getIdStringArray() {
        return new String[]{Long.toString(_id)};
    }

    public final Uri getContentUri() {
        return DatabaseHelper.getContentUri(getClass());
    }

    public final Uri getContentItemUri() {
        return hasId() ? ContentUris.withAppendedId(getContentUri(), _id) : null;
    }

    protected boolean hasId() {
        return _id != null && _id != UNDEFINED;
    }

    public Uri save(Context aContext) {
        ContentValues values = new ContentValues();
        populateContentValues(values);

        Uri uri = Uri.EMPTY;
        int updated = 0;
        if (hasId()) {
            uri = ContentUris.withAppendedId(getContentUri(), _id);
            updated = aContext.getContentResolver().update(uri, values, null, null);
            Timber.d("UPDATED: %s", uri);
        }
        if (updated == 0 || !hasId()) {
            uri = aContext.getContentResolver().insert(getContentUri(), values);
            Timber.d("INSERTED: %s", uri);
            try {
                _id = ContentUris.parseId(uri);
            } catch (Exception e) {
            }
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

    public static int bulkInsert(Context aContext, List<AbstractDataModel> aModelList) {
        ContentValues[] values = new ContentValues[aModelList.size()];
        int index = 0;
        Uri uri = null;
        for (AbstractDataModel model : aModelList) {
            values[index] = new ContentValues();
            model.populateContentValues(values[index++]);
            if (uri == null) {
                uri = model.getContentUri();
            } else if (!model.getContentUri().equals(uri)) {
                throw new IllegalStateException("models must all be of the same concrete type");
            }
        }
        int result = 0;
        if (index > 0 && uri != null)
            result = aContext.getContentResolver().bulkInsert(uri, values);
        return result;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(_id);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
