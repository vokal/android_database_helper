package com.vokal.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.BadParcelableException;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.*;
import java.util.*;

public class DatabaseHelper extends SQLiteOpenHelper {

    protected static final ArrayList<String>      TABLE_NAMES     = new ArrayList<String>();
    protected static final HashMap<Class, String> TABLE_MAP       = new HashMap<Class, String>();
    protected static final HashMap<Class, Uri>    CONTENT_URI_MAP = new HashMap<Class, Uri>();

    public DatabaseHelper(Context aContext, String aName, int aVersion) {
        super(aContext, aName, null, aVersion);
    }

    /*
     * registers a table with authority, uses lowercase class name as table name(s)
     */
    @SafeVarargs
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

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (Map.Entry<Class, String> entry : TABLE_MAP.entrySet()) {
            SQLiteTable.TableCreator creator = getTableCreator(entry.getKey());
            if (creator != null) {
                SQLiteTable.Builder builder = new SQLiteTable.Builder(entry.getValue());
                SQLiteTable table = creator.buildTableSchema(builder);
                db.execSQL(table.getCreateSQL());
                if (table.getIndicesSQL() != null) {
                    for (String indexSQL : table.getIndicesSQL()) {
                        db.execSQL(indexSQL);
                    }
                }
                if (table.getSeedValues() != null) {
                    for (ContentValues values : table.getSeedValues()) {
                        db.insert(table.getTableName(), table.getNullHack(), values);
                    }
                }
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Cursor tables = db.query("sqlite_master", null, "type='table'", null, null, null, null);
        ArrayList<String> tableNames = new ArrayList<String>();
        if (tables != null) {
            for (int i = 0; i < tables.getCount(); i++) {
                tables.moveToPosition(i);
                tableNames.add(tables.getString(tables.getColumnIndex("name")));
            }
        }
        Log.d("DatabaseHelper", "current tables: " + TextUtils.join(", ", tableNames));

        for (Map.Entry<Class, String> entry : TABLE_MAP.entrySet()) {
            SQLiteTable.TableCreator creator = getTableCreator(entry.getKey());
            if (creator != null) {
                SQLiteTable.Builder builder = new SQLiteTable.Builder(entry.getValue());
                SQLiteTable table = creator.buildTableSchema(builder);

                if (!tableNames.contains(table.getTableName())) {
                    db.execSQL(table.getCreateSQL());
                } else {
                    String[] updates = table.getUpdateSQL();
                    for (String updateSQL : updates) {
                        db.execSQL(updateSQL);
                    }
                }
                if (table.getIndicesSQL() != null) {
                    for (String indexSQL : table.getIndicesSQL()) {
                        db.execSQL(indexSQL);
                    }
                }
                if (table.getSeedValues() != null) {
                    for (ContentValues values : table.getSeedValues()) {
                        db.insert(table.getTableName(), table.getNullHack(), values);
                    }
                }
            }
        }
    }

    SQLiteTable.TableCreator getTableCreator(Class aModelClass) {
        String className = aModelClass.getSimpleName();
        SQLiteTable.TableCreator creator = null;
        try {
            Field f = aModelClass.getField("TABLE_CREATOR");
            creator = (SQLiteTable.TableCreator) f.get(null);
        } catch (ClassCastException e) {
            throw new IllegalStateException("ADHD protocol requires the object called TABLE_CREATOR " +
                                                    "on class " + className + " to be a SQLiteTable.TableCreator");
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("ADHD protocol requires a SQLiteTable.TableCreator " +
                                                    "object called TABLE_CREATOR on class " + className);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("ADHD protocol requires the TABLE_CREATOR object " +
                                                    "to be accessible on class " + className);
        } catch (NullPointerException e) {
            throw new IllegalStateException("ADHD protocol requires the TABLE_CREATOR " +
                                                    "object to be static on class " + className);
        }
        return creator;
    }
}
