package io.vokal.db;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.*;

public final class DatabaseHelper extends SQLiteOpenHelper {

    protected static final ArrayList<String>                        TABLE_NAMES     = new ArrayList<>();
    protected static final HashMap<Class, String>                   TABLE_MAP       = new HashMap<>();
    protected static final HashMap<String, Class>                   CLASS_MAP       = new HashMap<>();
    protected static final HashMap<Class, Uri>                      CONTENT_URI_MAP = new HashMap<>();
    protected static final HashMap<Class, SQLiteTable.TableCreator> TABLE_CREATORS  = new HashMap<>();

    private static boolean isOpen;

    DatabaseHelper(Context aContext, String aName, int aVersion) {
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

    public static int bulkInsert(Context aContext, List<? extends DataModelInterface> aModelList) {
        ContentValues[] values = new ContentValues[aModelList.size()];
        int index = 0;
        Uri uri = null;
        for (DataModelInterface model : aModelList) {
            values[index] = new ContentValues();
            model.populateContentValues(values[index++]);
            if (uri == null) {
                uri = DatabaseHelper.getContentUri(model.getClass());
            } else if (!DatabaseHelper.getContentUri(model.getClass()).equals(uri)) {
                throw new IllegalStateException("models must all be of the same concrete type to bulk insert");
            }
        }
        int result = 0;
        if (index > 0 && uri != null)
            result = aContext.getContentResolver().bulkInsert(uri, values);
        return result;
    }

    public static Uri save(Context aContext, DataModelInterface aModel) {
        ContentValues values = new ContentValues();
        aModel.populateContentValues(values);

        Uri uri = Uri.EMPTY;

        Uri content = getContentUri(aModel.getClass());
        int updated = aContext.getContentResolver().update(content, values, null, null);
        if (updated == 0) {
            uri = aContext.getContentResolver().insert(content, values);
        }

        return uri;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (Map.Entry<Class, String> entry : TABLE_MAP.entrySet()) {
            SQLiteTable.TableCreator creator = getTableCreator(entry.getKey());
            if (creator != null) {
                SQLiteTable.Builder builder = new SQLiteTable.Builder(entry.getValue());
                SQLiteTable table = creator.buildTableSchema(builder);
                if (table != null) {
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

        for (Map.Entry<Class, String> entry : TABLE_MAP.entrySet()) {
            SQLiteTable.TableCreator creator = getTableCreator(entry.getKey());
            if (creator == null) continue;

            SQLiteTable table = null;
            String tableName = entry.getValue();
            if (!tableNames.contains(tableName)) {
                SQLiteTable.Builder builder = new SQLiteTable.Builder(tableName);
                table = creator.buildTableSchema(builder);
                if (table != null) {
                    db.execSQL(table.getCreateSQL());
                }
            } else {
                SQLiteTable.Upgrader upgrader = new SQLiteTable.Upgrader(tableName);
                table = creator.updateTableSchema(upgrader, oldVersion);
                if (table != null) {
                    if (table.isCleanUpgrade()) {
                        SQLiteTable.Builder builder = new SQLiteTable.Builder(tableName);
                        table = creator.buildTableSchema(builder);
                        if (table != null) {
                            db.execSQL("DROP TABLE IF EXISTS " + tableName);
                            db.execSQL(table.getCreateSQL());
                        }
                    } else {
                        String[] updates = table.getUpdateSQL();
                        for (String updateSQL : updates) {
                            db.execSQL(updateSQL);
                        }
                    }
                }
            }

            if (table != null) {
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

    static @Nullable
    SQLiteTable.TableCreator getTableCreator(Class<?> aModelClass) {
        if (TABLE_CREATORS.containsKey(aModelClass)) {
            return TABLE_CREATORS.get(aModelClass);
        }

        SQLiteTable.TableCreator creator = null;

        try {
            Field f = aModelClass.getField("TABLE_CREATOR");
            creator = (SQLiteTable.TableCreator) f.get(null);
        } catch (Throwable e) {
            // continue
        }

        if (creator == null) {
            Object model = null;
            try {
                model = aModelClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                Log.e(aModelClass.getSimpleName(), "No default constructor " +
                        "or TABLE_CREATOR found on '" + aModelClass.getSimpleName() + "'!");
            }

            if (model != null) {
                if (model instanceof AbstractDataModel) {
                    creator = SQLiteTable.getDefaultCreator((AbstractDataModel) model);
                } else if (model instanceof DataModelInterface) {
                    creator = SQLiteTable.getDefaultCreator((DataModelInterface) model);
                }
            }
        }

        if (creator != null) {
            TABLE_CREATORS.put(aModelClass, creator);
        }

        return creator;
    }

    static List<String> getTableColumns(SQLiteDatabase aDatabase, String aTableName) {
        List<String> columns = new ArrayList<String>();

        if (isOpen && aDatabase != null) {
            Cursor c = aDatabase.rawQuery(String.format("PRAGMA table_info(%s)", aTableName), null);
            if (c.moveToFirst()) {
                do {
                    columns.add(c.getString(1));
                } while (c.moveToNext());
            }
            c.close();
        } else {
            Class tableClass = CLASS_MAP.get(aTableName);
            if (tableClass != null) {
                SQLiteTable.TableCreator creator = getTableCreator(tableClass);
                if (creator != null) {
                    SQLiteTable create = creator.buildTableSchema(new SQLiteTable.Builder(aTableName));
                    if (create != null) {
                        for (SQLiteTable.Column col : create.getColumns()) {
                            columns.add(col.name);
                        }
                    }
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

        return projection;
    }

    public static void wipeDatabase(Context aContext) {
        for (Uri uri : CONTENT_URI_MAP.values()) {
            aContext.getContentResolver().delete(uri, null, null);
        }
    }

}
