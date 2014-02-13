package com.vokal.database;

import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = DatabaseHelper.class.getSimpleName();

    protected static final SparseArray<String>            TABLE_NAMES    = new SparseArray<String>();
    protected static final SparseArray<SQLiteTable>       TABLES         = new SparseArray<SQLiteTable>();
    protected static final SparseArray<AbstractDataModel> MODELS         = new SparseArray<AbstractDataModel>();
    protected static final UriMatcher                     URI_MATCHER    = new UriMatcher(UriMatcher.NO_MATCH);
    protected static final UriMatcher                     URI_ID_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

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
        if (aContext.getApplicationInfo() != null){
            return aContext.getApplicationInfo().packageName.concat(".vokal.db");
        }
        return NAME;
    }
    /*
     * registers a table with provider, returns the content:// Uri
     */
    public static Uri registerModel(Class<? extends AbstractDataModel> aModelClass) {
        try {
            Constructor<? extends AbstractDataModel> ctor = aModelClass.getConstructor(new Class[]{});
            AbstractDataModel modelObj = ctor.newInstance(new Object[]{});
            String tableName = modelObj.getTableName();
            int index = TABLE_NAMES.indexOfValue(tableName);
            if (index < 0) {
                modelObj.setContentUri(Uri.parse(String.format("content://%s/%s", AUTHORITY, tableName)));
                SQLiteTable.Builder builder = new SQLiteTable.Builder(tableName);
                SQLiteTable table = modelObj.buildTableSchema(builder);
                modelObj.setTable(table);

                index = TABLES.size();
                TABLE_NAMES.append(index, tableName);
                TABLES.append(index, table);
                MODELS.append(index, modelObj);
                URI_MATCHER.addURI(AUTHORITY, tableName, index);
                URI_ID_MATCHER.addURI(AUTHORITY, tableName.concat("/#"), index);
            }

            return modelObj.getContentUri();
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

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (int i = 0; i < TABLES.size(); i++) {
            db.execSQL(TABLES.valueAt(i).getCreateSQL());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO
    }
}
