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

    protected static final ArrayList<String>      TABLE_NAMES     = new ArrayList<String>();
    protected static final HashMap<Class, String> TABLE_MAP       = new HashMap<Class, String>();
    protected static final HashMap<Class, Uri>    CONTENT_URI_MAP = new HashMap<Class, Uri>();

    public DatabaseHelper(Context aContext, String aName, int aVersion) {
        super(aContext, aName, null, aVersion);
    }

    /*
     * registers a table with authority, uses lowercase class name as table name
     */
    @DebugLog
    public static void registerModel(Context aContext, Class<? extends AbstractDataModel>... aModelClass) {
        if (aModelClass != null) {
            for (Class<? extends AbstractDataModel> clazz : aModelClass) {
                registerModel(aContext, clazz, clazz.getSimpleName().toLowerCase());
            }
        }
    }

    /*
     * registers a table with provider with authority using specified table name
     */
    @DebugLog
    public static void registerModel(Context aContext, Class<? extends AbstractDataModel> aModelClass, String aTableName) {
        if (!TABLE_MAP.values().contains(aTableName)) {
            int matcher_index = TABLE_NAMES.size();

            TABLE_NAMES.add(aTableName);
            TABLE_MAP.put(aModelClass, aTableName);

            String authority = SimpleContentProvider.getContentAuthority(aContext);
            Uri contentUri = Uri.parse(String.format("content://%s/%s", authority, aTableName));
            CONTENT_URI_MAP.put(aModelClass, contentUri);

            SimpleContentProvider.URI_MATCHER.addURI(authority, aTableName, matcher_index);
            SimpleContentProvider.URI_ID_MATCHER.addURI(authority, aTableName, matcher_index);
        }
    }

    public static Uri getContentUri(Class<? extends AbstractDataModel> aTableName) {
        return CONTENT_URI_MAP.get(aTableName);
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
