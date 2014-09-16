package com.vokal.db;

import android.content.*;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.vokal.db.util.CursorGetter;

public abstract class AbstractDataModel implements DataModelInterface, BaseColumns, Parcelable {

    public static final Creator<AbstractDataModel> CREATOR = new Creator<AbstractDataModel>() {
        @Override
        public AbstractDataModel createFromParcel(Parcel source) {
            String tableName = source.readString();
            if (SQLiteTable.isTableAbstractDataModel(tableName)) {
                Class<?> clazz = DatabaseHelper.CLASS_MAP.get(tableName);
                if (clazz != null) {
                    try {
                        Constructor constructor = clazz.getConstructor(Parcel.class);
                        return (AbstractDataModel) constructor.newInstance(source);
                    } catch (Exception e) {
                        Log.e(clazz.getSimpleName(), "No Parcel constructor, cannot un-parcel!");
                    }
                }
            }
            return new AbstractDataModel(source) {};
        }

        @Override
        public AbstractDataModel[] newArray(int size) {
            return new AbstractDataModel[size];
        }
    };

    private static final String   WHERE_ID = _ID + "=?";
    private final        String[] ID_ARG   = new String[1];

    protected transient long _id;

    protected AbstractDataModel() {}

    protected AbstractDataModel(Parcel aSource) {
        _id = aSource.readLong();
    }

    protected AbstractDataModel(CursorGetter aGetter) {
        if (aGetter.hasColumn(_ID) && !aGetter.isNull(_ID)) {
            _id = aGetter.getLong(_ID);
        }
    }

    /*
     * override to add your model field columns and constraints
     */
    @Override
    public SQLiteTable onTableCreate(SQLiteTable.Builder aBuilder) {
        return aBuilder.build();
    }

    /*
     * override to handle upgrades.  if you don't override, table will be dropped and re-created
     */
    @Override
    public SQLiteTable onTableUpgrade(SQLiteTable.Upgrader aUpgrader, int aOldVersion) {
        return null;
    }

    @Override
    public void populateContentValues(ContentValues aValues) {}

    public Uri save(Context aContext) {
        ContentValues values = new ContentValues();
        populateContentValues(values);
        if (hasId()) values.put(_ID, _id);

        int updated = 0;
        Uri uri = getContentItemUri();
        if (uri != null) {
            updated = aContext.getContentResolver().update(uri, values, null, null);
        } else {
            // TODO:
            //  - test above update
            // - fall back to SELECT w/ ARG
        }
        if (updated == 0 || !hasId()) {
            uri = aContext.getContentResolver().insert(getContentUri(), values);
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
            ID_ARG[0] = Long.toString(_id);
            result = aContext.getContentResolver().delete(getContentUri(), WHERE_ID, ID_ARG) == 1;
        }
        return result;
    }

    public final Uri getContentItemUri() {
        return hasId() ? ContentUris.withAppendedId(getContentUri(), _id) : null;
    }

    public final Uri getContentUri() {
        return DatabaseHelper.getContentUri(((Object) this).getClass());
    }

    public static int bulkInsert(Context aContext, List<? extends AbstractDataModel> aModelList) {
        ContentValues[] values = new ContentValues[aModelList.size()];
        int index = 0;
        Uri uri = null;
        for (AbstractDataModel model : aModelList) {
            values[index] = new ContentValues();
            if (model.hasId())
                values[index].put(_ID, model._id);
            model.populateContentValues(values[index++]);
            if (uri == null) {
                uri = model.getContentUri();
            } else if (!model.getContentUri().equals(uri)) {
                throw new IllegalStateException("models must all be of the same concrete type to bulk insert");
            }
        }
        int result = 0;
        if (index > 0 && uri != null)
            result = aContext.getContentResolver().bulkInsert(uri, values);
        return result;
    }

    private boolean hasId() {
        return _id > 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Class<?> clazz = ((Object) this).getClass();
        dest.writeString(DatabaseHelper.TABLE_MAP.get(clazz));
        dest.writeLong(_id);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
