package com.vokal.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import hugo.weaving.DebugLog;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = DatabaseHelper.class.getSimpleName();

    protected static final ArrayList<String>      TABLE_NAMES     = new ArrayList<String>();
    protected static final HashMap<Class, String> TABLE_MAP       = new HashMap<Class, String>();
    protected static final HashMap<Class, Uri>    CONTENT_URI_MAP = new HashMap<Class, Uri>();

    public DatabaseHelper(Context aContext, String aName, int aVersion, String authority) {
        super(aContext, aName, null, aVersion);

        int index = 0;
        for (Map.Entry<Class, String> entry : TABLE_MAP.entrySet()) {
            Uri contentUri = Uri.parse(String.format("content://%s/%s", authority, entry.getValue()));
            CONTENT_URI_MAP.put(entry.getKey(), contentUri);

            SimpleContentProvider.URI_MATCHER.addURI(authority, entry.getValue(), index);
            SimpleContentProvider.URI_ID_MATCHER.addURI(authority, entry.getValue().concat("/#"), index);
            index++;
        }
    }

    /*
     * registers a table with provider, return content Uri
     */
    @DebugLog
    public static void registerModel(Class<? extends AbstractDataModel> aModelClass) {
        registerModel(aModelClass, aModelClass.getSimpleName().toLowerCase());
    }

    public static Uri getContentUri(Class<? extends AbstractDataModel> aTableName) {
        return CONTENT_URI_MAP.get(aTableName);
    }

    /*
     * registers a table with provider with specified table name, returns content Uri
     */
    @DebugLog
    public static void registerModel(Class<? extends AbstractDataModel> aModelClass, String aTableName) {
        if (!TABLE_MAP.values().contains(aTableName)) {
            TABLE_NAMES.add(aTableName);
            TABLE_MAP.put(aModelClass, aTableName);
        }
    }

    @DebugLog
    @Override
    public void onCreate(SQLiteDatabase db) {
        for (Map.Entry<Class, String> entry : TABLE_MAP.entrySet()) {
            Class aModelClass = entry.getKey();
            String aTableName = entry.getValue();
            try {
                Constructor<? extends AbstractDataModel> ctor = aModelClass.getConstructor(new Class[]{});
                AbstractDataModel modelObj = ctor.newInstance(new Object[]{});

                SQLiteTable.Builder builder = new SQLiteTable.Builder(aTableName);
                SQLiteTable table = modelObj.buildTableSchema(builder);
                db.execSQL(table.getCreateSQL());
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @DebugLog
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
       // TODO
    }
}
