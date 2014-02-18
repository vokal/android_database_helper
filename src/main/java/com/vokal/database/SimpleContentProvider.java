package com.vokal.database;

import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.*;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

import static android.text.TextUtils.isEmpty;

public class SimpleContentProvider extends ContentProvider {

    private static final String TAG = SimpleContentProvider.class.getSimpleName();
    private static final String KEY_DB_NAME = "database_name";
    private static final String KEY_DB_VERSION = "database_version";

    static final UriMatcher URI_MATCHER    = new UriMatcher(UriMatcher.NO_MATCH);
    static final UriMatcher URI_ID_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static String AUTHORITY = "com.vokal.database";
    static String DB_NAME   = "vokal.db";
    static int    DB_VERSION = 1;

    protected DatabaseHelper mHelper;

    @Override
    public boolean onCreate() {
        Log.i(TAG, "onCreate() called...");
        ComponentName comp = new ComponentName(getContext(), getClass());
        try {
            ProviderInfo pi = getContext().getPackageManager().getProviderInfo(comp, PackageManager.GET_META_DATA);
            if (pi != null) {
                if (!isEmpty(pi.authority)) AUTHORITY = pi.authority;
                if (pi.metaData != null) {
                    if (pi.metaData.containsKey(KEY_DB_NAME))
                        DB_NAME = pi.metaData.getString(KEY_DB_NAME);
                    DB_VERSION = pi.metaData.getInt(KEY_DB_VERSION, DB_VERSION);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "AUTHORITY: " + AUTHORITY);
        mHelper = new DatabaseHelper(getContext(), DB_NAME, DB_VERSION, AUTHORITY);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }


    String getTableFromUri(Uri aUri) {
        int match = URI_MATCHER.match(aUri);
        if (match == -1) {
            match = URI_ID_MATCHER.match(aUri);
            if (match == -1) {
                Log.w(TAG, "no table for: " + aUri);
            }
        }
        if (match >= 0) {
            return DatabaseHelper.TABLE_NAMES.get(match);
        }
        return null;
    }

    @Override
    public Uri insert(Uri aUri, ContentValues aValues) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String table = getTableFromUri(aUri);

        long id = db.insertWithOnConflict(table, "", aValues, SQLiteDatabase.CONFLICT_REPLACE);

        if (id > -1) {
            Uri rowUri = ContentUris.withAppendedId(aUri, id);

            getContext().getContentResolver().notifyChange(aUri, null);
            return rowUri;
        }

        throw new SQLException("Failed to insert row into " + aUri);
    }

    @Override
    public int update(Uri aUri, ContentValues aValues, String aSelection, String[] aSelectionArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String table = getTableFromUri(aUri);

        int result = db.update(table, aValues, aSelection, aSelectionArgs);

        getContext().getContentResolver().notifyChange(aUri, null);

        return result;
    }


    @Override
    public int bulkInsert(Uri aUri, ContentValues[] aValues) {
        String table = getTableFromUri(aUri);
        int result = 0;

        int conflictRule = SQLiteDatabase.CONFLICT_REPLACE;

        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            for (ContentValues value : aValues) {
                db.insertWithOnConflict(table, "", value, conflictRule);
                result++;
            }

            db.setTransactionSuccessful();
            if (result > 0) {
                getContext().getContentResolver().notifyChange(aUri, null);
            }
        } catch (SQLiteConstraintException e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }

        return result;
    }

    @Override
    public Cursor query(Uri aUri, String[] aProjection, String aSelection, String[] aSelectionArgs, String aSortOrder) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        SQLiteDatabase db = mHelper.getReadableDatabase();

        builder.setTables(getTableFromUri(aUri));

        Cursor result = builder.query(db, aProjection, aSelection,
                                      aSelectionArgs, null, null, aSortOrder);
        if (result != null) {
            result.setNotificationUri(getContext().getContentResolver(), aUri);
        }

        return result;
    }

    @Override
     public int delete(Uri aUri, String aSelection, String[] aSelectionArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();

        String table = getTableFromUri(aUri);

        getContext().getContentResolver().notifyChange(aUri, null);

        return db.delete(table, aSelection, aSelectionArgs);
    }


}
