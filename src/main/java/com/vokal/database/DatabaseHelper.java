package com.vokal.database;

import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import hugo.weaving.DebugLog;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = DatabaseHelper.class.getSimpleName();

    protected static final ArrayList<String> TABLE_NAMES = new ArrayList<String>();

    protected static final HashMap<String, AbstractDataModel> MODELS = new HashMap<String, AbstractDataModel>();

    protected static final UriMatcher URI_MATCHER    = new UriMatcher(UriMatcher.NO_MATCH);
    protected static final UriMatcher URI_ID_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    protected static final String AUTHORITY = "com.vokal.database";
    private static final   String NAME      = "vokal.db";
    private static final   int    VERSION   = 1;

    public DatabaseHelper(Context context) {
        super(context, getDefaultName(context), null, VERSION);
    }

    public DatabaseHelper(Context aContext, String aName) {
        super(aContext, aName, null, VERSION);
    }

    public DatabaseHelper(Context aContext, String aName, int aVersion) {
        super(aContext, aName, null, aVersion);
    }

    private static String getDefaultName(Context aContext) {
        if (aContext.getApplicationInfo() != null) {
            return aContext.getApplicationInfo().packageName.concat(".vokal.db");
        }
        return NAME;
    }

    /*
     * registers a table with provider, return content Uri
     */
    @DebugLog
    public static Uri registerModel(Class<? extends AbstractDataModel> aModelClass) {
        return registerModel(aModelClass, aModelClass.getSimpleName().toLowerCase());
    }

    /*
     * registers a table with provider with specified table name, returns content Uri
     */
    @DebugLog
    public static Uri registerModel(Class<? extends AbstractDataModel> aModelClass, String aTableName) {
        try {
            if (!TABLE_NAMES.contains(aTableName)) {
                int index = TABLE_NAMES.size();
                URI_MATCHER.addURI(AUTHORITY, aTableName, index);
                URI_ID_MATCHER.addURI(AUTHORITY, aTableName.concat("/#"), index);
                TABLE_NAMES.add(aTableName);

                Constructor<? extends AbstractDataModel> ctor = aModelClass.getConstructor(new Class[]{});
                AbstractDataModel modelObj = ctor.newInstance(new Object[]{});
                MODELS.put(aTableName, modelObj);

                Uri contentUri = Uri.parse(String.format("content://%s/%s", AUTHORITY, aTableName));
                modelObj.setContentUri(contentUri);
                return contentUri;
            } else {
                return MODELS.get(aTableName).getContentUri();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
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
            return TABLE_NAMES.get(match);
        }
        return null;
    }

    @DebugLog
    @Override
    public void onCreate(SQLiteDatabase db) {
        for (String aTableName : TABLE_NAMES) {
            AbstractDataModel model = MODELS.get(aTableName);
            SQLiteTable.Builder builder = new SQLiteTable.Builder(aTableName);
            SQLiteTable table = model.buildTableSchema(builder);
            db.execSQL(table.getCreateSQL());
            model.setTable(table);  // TODO: query columns in case raw SQL was set to create
        }
    }

    @DebugLog
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (String aTableName : TABLE_NAMES) {
            AbstractDataModel model = MODELS.get(aTableName);
            SQLiteTable.Builder builder = new SQLiteTable.Builder(aTableName);
            SQLiteTable table = model.updateTableSchema(builder, oldVersion);
            if (table != null) {
//            db.execSQL(table.getUpdateSQL());  // TODO
            }
        }
    }
}
