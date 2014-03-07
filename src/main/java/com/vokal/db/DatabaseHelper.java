package com.vokal.db;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.*;
import java.util.*;

import timber.log.Timber;

public class DatabaseHelper extends SQLiteOpenHelper {

    protected static final ArrayList<String>                 TABLE_NAMES     = new ArrayList<String>();
    protected static final HashMap<Class, String>            TABLE_MAP       = new HashMap<Class, String>();
    protected static final HashMap<String, Class>            CLASS_MAP       = new HashMap<String, Class>();
    protected static final HashMap<Class, Uri>               CONTENT_URI_MAP = new HashMap<Class, Uri>();

    private static boolean isOpen;

    public DatabaseHelper(Context aContext, String aName, int aVersion) {
        super(aContext, aName, null, aVersion);
    }

    /*
     * registers a table with authority, uses lowercase class name as table name(s)
     */
    @SafeVarargs
    public static void registerModel(Context aContext, Class<?>... aModelClass) {
        if (aModelClass != null) {
            for (Class<?> clazz : aModelClass) {
                registerModel(aContext, clazz, clazz.getSimpleName().toLowerCase(Locale.getDefault()));
            }
        }
    }

    /*
     * registers a table with provider with authority using specified table name
     */
    public static void registerModel(Context aContext, Class<?> aModelClass, String aTableName) {
        if (!TABLE_NAMES.contains(aTableName)) {
            int matcher_index = TABLE_NAMES.size();

            TABLE_NAMES.add(aTableName);
            TABLE_MAP.put(aModelClass, aTableName);
            CLASS_MAP.put(aTableName, aModelClass);

            String authority = SimpleContentProvider.getContentAuthority(aContext);
            Uri contentUri = Uri.parse(String.format("content://%s/%s", authority, aTableName));
            CONTENT_URI_MAP.put(aModelClass, contentUri);

            SimpleContentProvider.URI_MATCHER.addURI(authority, aTableName, matcher_index);
            SimpleContentProvider.URI_ID_MATCHER.addURI(authority, aTableName, matcher_index);
        }
    }

    public static Uri getContentUri(Class<?> aTableName) {
        return CONTENT_URI_MAP.get(aTableName);
    }

    public static Uri getJoinedContentUri(Class<?> aTable1, String aColumn1,
                                          Class<?> aTable2, String aColumn2) {
        return getJoinedContentUri(aTable1, aColumn1, aTable2, aColumn2, null);
    }

    public static Uri getJoinedContentUri(Class<?> aTable1, String aColumn1,
                                          Class<?> aTable2, String aColumn2,
                                          Map<String, String> aProjMap) {
        String auth = SimpleContentProvider.sContentAuthority;
        if (auth == null) throw new IllegalStateException("Register tables with registerModel(..) methods first.");
        String tblName1 = TABLE_MAP.get(aTable1);
        if (tblName1 == null) throw new IllegalStateException("call registerModel() first for table " + aTable1);
        String tblName2 = TABLE_MAP.get(aTable2);
        if (tblName2 == null) throw new IllegalStateException("call registerModel() first for table " + aTable2);

        String path = tblName1 + "_" + tblName2;
        Uri contentUri = Uri.parse(String.format("content://%s/%s", auth, path));
        int exists = SimpleContentProvider.URI_JOIN_MATCHER.match(contentUri);
        if (exists != UriMatcher.NO_MATCH) {
            return contentUri;
        }

        String table = String.format("%s LEFT OUTER JOIN %s ON (%s.%s = %s.%s)",
                                     tblName1, tblName2,
                                     tblName1, aColumn1,
                                     tblName2, aColumn2);

        int nextIndex = SimpleContentProvider.JOIN_TABLES.size();
        SimpleContentProvider.JOIN_TABLES.add(table);
        SimpleContentProvider.URI_JOIN_MATCHER.addURI(auth, path, nextIndex);

        SimpleContentProvider.PROJECTION_MAPS.put(contentUri, aProjMap);
        SimpleContentProvider.JOIN_DETAILS.add(new SimpleContentProvider.Join(contentUri,
                                                                              tblName1, aColumn1,
                                                                              tblName2, aColumn2));

        return contentUri;
    }


    public static void setProjectionMap(Uri aContentUri, Map<String, String> aProjectionMap) {
        SimpleContentProvider.PROJECTION_MAPS.put(aContentUri, aProjectionMap);
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

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        isOpen = true;
    }

    static SQLiteTable.TableCreator getTableCreator(Class aModelClass) {
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

    static List<String> getTableColumns(SQLiteDatabase aDatabase, String aTableName) {
        List<String> columns = new ArrayList<String>();
        if (isOpen && aDatabase != null) {
            Timber.d("getting '%s' columns from database...", aTableName);
            Cursor c = aDatabase.rawQuery(String.format("PRAGMA table_info(%s)", aTableName), null);
            if (c.moveToFirst()) {
                do {
                    columns.add(c.getString(1));
                } while (c.moveToNext());
            }
        } else {
            Timber.d("getting '%s' columns from creator/updater", aTableName);
            Class tableClass = CLASS_MAP.get(aTableName);
            if (tableClass != null) {
                SQLiteTable.TableCreator creator = getTableCreator(tableClass);
                SQLiteTable create = creator.buildTableSchema(new SQLiteTable.Builder(aTableName));
                for (SQLiteTable.Column col : create.getColumns()) {
                    columns.add(col.name);
                }
                SQLiteTable upgrade = creator.updateTableSchema(new SQLiteTable.Updater(aTableName),
                                                                SimpleContentProvider.sDatabaseVersion);
                for (SQLiteTable.Column col : upgrade.getColumns()) {
                    columns.add(col.name);
                }
            }
        }
        return columns;
    }

    static Map<String,String> buildDefaultJoinMap(SimpleContentProvider.Join aJoin, SQLiteDatabase aDb) {
        Map<String, String> projection = new HashMap<String, String>();

        List<String> columns1 = getTableColumns(aDb, aJoin.table_1);
        List<String> columns2 = getTableColumns(aDb, aJoin.table_2);

        projection.put("_id", aJoin.table_1.concat("._id as _id"));

        for (String col : columns1) {
            projection.put(String.format("%s_%s", aJoin.table_1, col),
                           String.format("%s.%s AS %s_%s", aJoin.table_1, col, aJoin.table_1, col));

        }
        for (String col : columns2) {
            projection.put(String.format("%s_%s", aJoin.table_2, col),
                        String.format("%s.%s AS %s_%s", aJoin.table_2, col, aJoin.table_2, col));
        }

        Timber.d("built default join projection map: %s", projection);

        return projection;
    }

}
