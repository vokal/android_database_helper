package com.vokal.database;

import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.*;
import android.database.sqlite.*;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import static android.text.TextUtils.isEmpty;

public class SimpleContentProvider extends ContentProvider {

    private static final String TAG  = SimpleContentProvider.class.getSimpleName();
    private static final String NAME = SimpleContentProvider.class.getCanonicalName();

    private static final String KEY_DB_NAME    = "database_name";
    private static final String KEY_DB_VERSION = "database_version";

    private static final String DEFAULT_AUTHORITY = "com.vokal.database";
    private static final String DEFAULT_NAME      = "vokal.db";

    static final UriMatcher URI_MATCHER    = new UriMatcher(UriMatcher.NO_MATCH);
    static final UriMatcher URI_ID_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private static ProviderInfo sProviderInfo;

    static String sContentAuthority;
    static String sDatabaseName;
    static int sDatabaseVersion = 1;

    protected DatabaseHelper mHelper;

    public static String getContentAuthority(Context aContext) {
        if (sContentAuthority == null) {
            sContentAuthority = DEFAULT_AUTHORITY;
            try {
                getProviderInfo(aContext);
                if (sProviderInfo != null) {
                    if (!isEmpty(sProviderInfo.authority))
                        sContentAuthority = sProviderInfo.authority;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return sContentAuthority;
    }

    private static void getProviderInfo(Context aContext)
            throws ClassNotFoundException, PackageManager.NameNotFoundException {
        if (sProviderInfo == null) {
            Class<?> clazz = Class.forName(NAME);
            ComponentName component = new ComponentName(aContext, clazz);
            PackageManager pm = aContext.getPackageManager();
            if (pm != null) {
                sProviderInfo = pm.getProviderInfo(component, PackageManager.GET_META_DATA);
            }
        }
    }

    @Override
    public boolean onCreate() {
        getContentAuthority(getContext());
        if (sProviderInfo != null && sProviderInfo.metaData != null) {
            sDatabaseName = DEFAULT_NAME;
            if (sProviderInfo.metaData.containsKey(KEY_DB_NAME)) {
                sDatabaseName = sProviderInfo.metaData.getString(KEY_DB_NAME);
            }
            sDatabaseVersion = sProviderInfo.metaData.getInt(KEY_DB_VERSION, sDatabaseVersion);
        }

        mHelper = new DatabaseHelper(getContext(), sDatabaseName, sDatabaseVersion);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    public int getConflictRule(String aTableName) {
        return SQLiteDatabase.CONFLICT_REPLACE;
    }

    @Override
    public Cursor query(Uri aUri, String[] aProjection, String aSelection, String[] aSelectionArgs, String aSortOrder) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        SQLiteDatabase db = mHelper.getReadableDatabase();
        assert db != null;

        Cursor result = null;
        UriMatch match = getTableFromUri(aUri);
        if (match != null) {
            builder.setTables(match.table);

            String where = aSelection;
            String[] args = aSelectionArgs;
            if (match.item) {
//                builder.appendWhere("_ID=" + aUri.getLastPathSegment());
                where = DatabaseUtils.concatenateWhere(where, "_ID=?");
                args = DatabaseUtils.appendSelectionArgs(args, new String[]{aUri.getLastPathSegment()});
            }

            result = builder.query(db, aProjection, where, args, null, null, aSortOrder);

            /*if (result != null) {
                Context ctx = getContext();
                assert ctx != null;
                result.setNotificationUri(ctx.getContentResolver(), aUri);
            }*/
        }

        return result;
    }

    @Override
    public Uri insert(Uri aUri, ContentValues aValues) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        assert db != null;

        Uri result = null;
        UriMatch match = getTableFromUri(aUri);
        if (match != null) {
            long id = db.insertWithOnConflict(match.table, "", aValues, getConflictRule(match.table));

            if (id > -1) {
                Context ctx = getContext();
                assert ctx != null;
                ctx.getContentResolver().notifyChange(aUri, null);

                result = ContentUris.withAppendedId(aUri, id);
            }
        }

        return result;
    }

    @Override
    public int update(Uri aUri, ContentValues aValues, String aSelection, String[] aSelectionArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        assert db != null;

        int result = 0;
        UriMatch match = getTableFromUri(aUri);
        if (match != null) {
            String where = aSelection;
            String[] args = aSelectionArgs;
            if (match.item) {
                where = DatabaseUtils.concatenateWhere(where, "_ID=?");
                args = DatabaseUtils.appendSelectionArgs(args, new String[] {aUri.getLastPathSegment()});
            }

            result = db.update(match.table, aValues, where, args);

            if (result > 0) {
                Context ctx = getContext();
                assert ctx != null;
                ctx.getContentResolver().notifyChange(aUri, null);
            }
        }
        return result;
    }

    @Override
    public int bulkInsert(Uri aUri, ContentValues[] aValues) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        assert db != null;

        int result = 0;
        UriMatch match = getTableFromUri(aUri);
        if (match != null) {
            db.beginTransaction();

            try {
                for (ContentValues value : aValues) {
                    db.insertWithOnConflict(match.table, "", value, getConflictRule(match.table));
                    result++;
                }

                db.setTransactionSuccessful();
            } catch (SQLiteConstraintException e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();

                if (result > 0) {
                    Context ctx = getContext();
                    assert ctx != null;
                    ctx.getContentResolver().notifyChange(aUri, null);
                }
            }
        }

        return result;
    }


    @Override
    public int delete(Uri aUri, String aSelection, String[] aSelectionArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        assert db != null;

        int result = 0;
        UriMatch match = getTableFromUri(aUri);
        if (match != null) {
            String where = aSelection;
            String[] args = aSelectionArgs;
            if (match.item) {
                where = DatabaseUtils.concatenateWhere(where, "_ID=?");
                args = DatabaseUtils.appendSelectionArgs(args, new String[] {aUri.getLastPathSegment()});
            }

            result = db.delete(match.table, where, args);

            Context ctx = getContext();
            assert ctx != null;
            getContext().getContentResolver().notifyChange(aUri, null);
        }

        return result;
    }

    protected UriMatch getTableFromUri(Uri aUri) {
        UriMatch match = new UriMatch();
        int index = URI_MATCHER.match(aUri);
        if (index == UriMatcher.NO_MATCH) {
            index = URI_ID_MATCHER.match(aUri);
            if (index != UriMatcher.NO_MATCH) {
                match.item = true;
            }
        }
        if (index >= 0) {
            match.table = DatabaseHelper.TABLE_NAMES.get(index);
        }
        return match;
    }

    protected class UriMatch {
        String table;
        boolean item;
    }

}
