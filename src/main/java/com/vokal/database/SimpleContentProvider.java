package com.vokal.database;

import android.content.*;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.*;
import android.net.Uri;

public class SimpleContentProvider extends ContentProvider {

    protected DatabaseHelper mHelper;

    @Override
    public boolean onCreate() {
        mHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri aUri, ContentValues aValues) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String table = mHelper.getTableFromUri(aUri);

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
        String table = mHelper.getTableFromUri(aUri);

        int result = db.update(table, aValues, aSelection, aSelectionArgs);

        getContext().getContentResolver().notifyChange(aUri, null);

        return result;
    }


    @Override
    public int bulkInsert(Uri aUri, ContentValues[] aValues) {
        String table = mHelper.getTableFromUri(aUri);
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

        builder.setTables(mHelper.getTableFromUri(aUri));

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

        String table = mHelper.getTableFromUri(aUri);

        getContext().getContentResolver().notifyChange(aUri, null);

        return db.delete(table, aSelection, aSelectionArgs);
    }


}
