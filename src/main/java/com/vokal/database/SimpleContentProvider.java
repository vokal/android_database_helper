package com.vokal.database;

import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.*;
import android.database.sqlite.*;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;

import static android.text.TextUtils.isEmpty;

public class SimpleContentProvider extends SQLiteContentProvider {

    private static final String TAG  = SimpleContentProvider.class.getSimpleName();
    private static final String NAME = SimpleContentProvider.class.getCanonicalName();

    private static final String KEY_DB_NAME    = "database_name";
    private static final String KEY_DB_VERSION = "database_version";

    private static final String DEFAULT_AUTHORITY = "com.vokal.database";
    private static final String DEFAULT_NAME      = "vokal.db";

    static final UriMatcher URI_MATCHER      = new UriMatcher(UriMatcher.NO_MATCH);
    static final UriMatcher URI_ID_MATCHER   = new UriMatcher(UriMatcher.NO_MATCH);
    static final UriMatcher URI_JOIN_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static final ArrayList<String> JOIN_TABLES = new ArrayList<String>();

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
        return super.onCreate();
    }

    @Override
    protected SQLiteOpenHelper getDatabaseHelper(Context context) {
        return mHelper;
    }

    @Override
    protected Uri insertInTransaction(Uri aUri, ContentValues aValues) {
        Uri result = null;
        UriMatch match = getTableFromUri(aUri);
        if (match != null) {
            if (match.join)
                return null;

            // TODO: store table null hacks
            long id = mDb.insertWithOnConflict(match.table, "", aValues, getConflictRule(match.table));

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
    protected int updateInTransaction(Uri aUri, ContentValues aValues, String aSelection, String[] aSelectionArgs) {
        int result = 0;
        UriMatch match = getTableFromUri(aUri);
        if (match != null) {
            if (match.join) return 0;

            String where = aSelection;
            String[] args = aSelectionArgs;
            if (match.item) {
                where = DatabaseUtils.concatenateWhere(where, "_ID=?");
                args = DatabaseUtils.appendSelectionArgs(args, new String[] {aUri.getLastPathSegment()});
            }

            result = mDb.update(match.table, aValues, where, args);

            if (result > 0) {
                Context ctx = getContext();
                assert ctx != null;
                ctx.getContentResolver().notifyChange(aUri, null);
            }
        }
        return result;
    }

    @Override
    protected int deleteInTransaction(Uri aUri, String aSelection, String[] aSelectionArgs) {
        int result = 0;
        UriMatch match = getTableFromUri(aUri);
        if (match != null) {
            if (match.join) return 0;

            String where = aSelection;
            String[] args = aSelectionArgs;
            if (match.item) {
                where = DatabaseUtils.concatenateWhere(where, "_ID=?");
                args = DatabaseUtils.appendSelectionArgs(args, new String[] {aUri.getLastPathSegment()});
            }

            result = mDb.delete(match.table, where, args);

            Context ctx = getContext();
            assert ctx != null;
            getContext().getContentResolver().notifyChange(aUri, null);
        }

        return result;
    }

    @Override
    protected void notifyChange() {
        // ???
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

            String[] args = aSelectionArgs;
            if (match.item) {
                builder.appendWhere(BaseColumns._ID + "=?");
                args = DatabaseUtils.appendSelectionArgs(args, new String[]{aUri.getLastPathSegment()});
            }
            result = builder.query(db, aProjection, aSelection, args, null, null, aSortOrder);

            if (result != null) {
                Context ctx = getContext();
                assert ctx != null;
                result.setNotificationUri(ctx.getContentResolver(), aUri);
            }
        }

        return result;
    }

    protected UriMatch getTableFromUri(Uri aUri) {
        UriMatch match = new UriMatch();

        // check for table match
        int index = URI_MATCHER.match(aUri);

        // check for item match
        if (index == UriMatcher.NO_MATCH) {
            index = URI_ID_MATCHER.match(aUri);
            if (index != UriMatcher.NO_MATCH) {
                match.item = true;
            }
        }

        // check for join match
        if (index == UriMatcher.NO_MATCH) {
            index = URI_JOIN_MATCHER.match(aUri);
            if (index != UriMatcher.NO_MATCH) {
                match.join = true;
                match.table = JOIN_TABLES.get(index);
                return match;
            }
        }

        if (index >= 0) {
            match.table = DatabaseHelper.TABLE_NAMES.get(index);
            return match;
        }

        return null;
    }

    protected class UriMatch {
        String table;
        boolean item;
        boolean join;
    }

}
