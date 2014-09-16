package com.vokal.db;

import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.*;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.util.*;

import static android.text.TextUtils.isEmpty;

public class SimpleContentProvider extends SQLiteContentProvider {

    private static final String NAME = SimpleContentProvider.class.getCanonicalName();

    private static final String KEY_DB_NAME    = "database_name";
    private static final String KEY_DB_VERSION = "database_version";

    private static final String DEFAULT_AUTHORITY = "com.vokal.database";
    private static final String DEFAULT_NAME      = "vokal.db";

    static final UriMatcher URI_MATCHER      = new UriMatcher(UriMatcher.NO_MATCH);
    static final UriMatcher URI_ID_MATCHER   = new UriMatcher(UriMatcher.NO_MATCH);
    static final UriMatcher URI_JOIN_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static final ArrayList<String> JOIN_TABLES  = new ArrayList<String>();
    static final ArrayList<Join>   JOIN_DETAILS = new ArrayList<Join>();

    static final HashMap<Uri, Map<String, String>> PROJECTION_MAPS = new HashMap<Uri, Map<String, String>>();

    static ProviderInfo sProviderInfo;
    static String       sContentAuthority;
    static String       sDatabaseName;
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
            } catch (UnsupportedOperationException e) {
                e.printStackTrace();
            }
        }
        return sContentAuthority;
    }

    private static void getProviderInfo(Context aContext)
            throws ClassNotFoundException, PackageManager.NameNotFoundException, UnsupportedOperationException{
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
                where = concatenateWhere(where, "_ID=?");
                args = appendSelectionArgs(args, new String[] {aUri.getLastPathSegment()});
            }

            result = mDb.update(match.table, aValues, where, args);
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
                where = concatenateWhere(where, "_ID=?");
                args = appendSelectionArgs(args, new String[] {aUri.getLastPathSegment()});
            }

            result = mDb.delete(match.table, where, args);
        }

        return result;
    }

    @Override
    protected void notifyChange() {
        ContentResolver resolver = getContext().getContentResolver();
        Uri[] uris = getNotificationUris();
        for (Uri uri : uris) {
            if (uri == null) continue;
            resolver.notifyChange(uri, null);
        }
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
                String id = aUri.getLastPathSegment();
                if (id != null) {
                    builder.appendWhere(BaseColumns._ID + "=?");
                    args = appendSelectionArgs(args, new String[]{id});
                }
            }

            Map<String, String> projection = PROJECTION_MAPS.get(aUri);
            if (match.join && projection == null) {
                projection = DatabaseHelper.buildDefaultJoinMap(JOIN_DETAILS.get(match.index), db);
                PROJECTION_MAPS.put(JOIN_DETAILS.get(match.index).base_uri, projection);
            }
            if (projection != null) builder.setProjectionMap(projection);

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
        match.index = URI_MATCHER.match(aUri);

        // check for item match
        if (match.index == UriMatcher.NO_MATCH) {
            match.index = URI_ID_MATCHER.match(aUri);
            if (match.index != UriMatcher.NO_MATCH) {
                match.item = true;
            }
        }

        if (match.index != UriMatcher.NO_MATCH) {
            match.table = DatabaseHelper.TABLE_NAMES.get(match.index);
            return match;
        }

        // check for join match
        match.index = URI_JOIN_MATCHER.match(aUri);
        if (match.index != UriMatcher.NO_MATCH) {
            match.table = JOIN_TABLES.get(match.index);
            match.join = true;
            return match;
        }

        return null;
    }

    protected class UriMatch {
        int     index;
        String  table;
        boolean item;
        boolean join;
    }

    static class Join {
        Uri base_uri;
        String table_1;
        String column_1;
        String table_2;
        String column_2;

        Join(Uri aBaseUri, String aTable1, String aColumn1, String aTable2, String aColumn2) {
            base_uri = aBaseUri;
            table_1 = aTable1;
            column_1 = aColumn1;
            table_2 = aTable2;
            column_2 = aColumn2;
        }
    }

    /**
     * Concatenates two SQL WHERE clauses, handling empty or null values.
     */
    public static String concatenateWhere(String a, String b) {
        if (TextUtils.isEmpty(a)) {
            return b;
        }
        if (TextUtils.isEmpty(b)) {
            return a;
        }

        return "(" + a + ") AND (" + b + ")";
    }

    /**
     * Appends one set of selection args to another. This is useful when adding a selection
     * argument to a user provided set.
     */
    public static String[] appendSelectionArgs(String[] originalValues, String[] newValues) {
        if (originalValues == null || originalValues.length == 0) {
            return newValues;
        }
        String[] result = new String[originalValues.length + newValues.length ];
        System.arraycopy(originalValues, 0, result, 0, originalValues.length);
        System.arraycopy(newValues, 0, result, originalValues.length, newValues.length);
        return result;
    }

    @Override
    public void shutdown() {
        mHelper.close();
    }
}
