package com.vokal.db;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.ProviderTestCase2;
import android.text.TextUtils;
import android.util.Log;

import java.util.*;

import com.vokal.db.test.models.*;
import com.vokal.db.util.CursorGetter;

public class DatabaseHelperTests extends ProviderTestCase2<SimpleContentProvider> {

    public DatabaseHelperTests() {
        super(SimpleContentProvider.class, "com.vokal.database");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getMockContext();
        DatabaseHelper.registerModel(mContext, ExtendedOne.class, ExtendedTwo.class, TestInterface.class);
    }

    public void testIsAbstractDAtaModel() {
        assertTrue(AbstractDataModel.class.isAssignableFrom(ExtendedOne.class));
        assertTrue(SQLiteTable.isTableAbstractDataModel("extendedone"));
    }

    public void testIsDataModelInterface() {
        assertTrue(DataModelInterface.class.isAssignableFrom(TestInterface.class));

        TestInterface i = new TestInterface();
        assertTrue(DataModelInterface.class.isAssignableFrom(i.getClass()));

        assertTrue(SQLiteTable.isTableDataModelInterface("testinterface"));
    }

    public void testGetTableNames() {
        List<String> columns = DatabaseHelper.getTableColumns(null, "extendedone");
        assertEquals("wrong number of columns", 6, columns.size());

        Set<String> unique = new HashSet<String>();
        for (String col : columns) {
            assertFalse("duplicate column:" + col, unique.contains(col));
            unique.add(col);
        }

        Log.d("DB", "ExtendedOne columns: " + TextUtils.join(", ", columns));
    }

    public void testGetTableNamesFromPragma() {
        DatabaseHelper helper = new DatabaseHelper(getMockContext(), "pragma.db", 1);
        SQLiteDatabase db = helper.getReadableDatabase();
        List<String> columns = DatabaseHelper.getTableColumns(db, "extendedtwo");
        assertEquals("wrong number of columns", 8, columns.size());

        Set<String> unique = new HashSet<String>();
        for (String col : columns) {
            assertFalse("duplicate column:" + col, unique.contains(col));
            unique.add(col);
        }

        Log.d("DB", "ExtendedTwo columns: " + TextUtils.join(", ", columns));
    }

    public void testTableCreate() throws Exception {
        SQLiteTable.TableCreator creator = DatabaseHelper.getTableCreator(TestInterface.class);
        assertNotNull(creator);

        SQLiteTable.Builder builder = new SQLiteTable.Builder("testinterface");
        SQLiteTable table = creator.buildTableSchema(builder);

        ContentResolver resolver = newResolverWithContentProviderFromSql(getMockContext(), "",
                                                                         SimpleContentProvider.class,
                                                                         "com.vokal.database",
                                                                         "create.db", 1,
                                                                         table.getCreateSQL());

        TestInterface i = new TestInterface();
        i.setLong(7);
        i.setString("created");
        i.setDate(new Date());
        ContentValues v = new ContentValues();
        i.populateContentValues(v);
        Uri result = resolver.insert(DatabaseHelper.getContentUri(TestInterface.class), v);
        assertNotNull(result);

        Cursor c = resolver.query(DatabaseHelper.getContentUri(TestInterface.class),
                                  null, null, null, null);

        assertNotNull(c);
        assertEquals(1, c.getCount());
        assertTrue(c.moveToFirst());

        CursorGetter getter = new CursorGetter(c);
        TestInterface t = new TestInterface(getter);
        assertEquals("created", t.getString());
        assertEquals(7, t.getLong());
    }

    public void testTableUpgrade() throws Exception {
        TestInterface i = new TestInterface();
        i.setLong(7);
        i.setString("created");
        i.setDate(new Date());
        ContentValues v = new ContentValues();
        i.populateContentValues(v);

        DatabaseHelper helper = new DatabaseHelper(getMockContext(), "helper.db", 1);
        SQLiteDatabase db = helper.getWritableDatabase();
        long result = db.insert("testinterface", null, v);
        assertTrue(result > 0);

        TestInterface.sDBVersion = 2;
        i.setString("upgraded");
        i.newString = "new string";
        v = new ContentValues();
        i.populateContentValues(v);

        helper = new DatabaseHelper(getMockContext(), "helper.db", 2);
        db = helper.getWritableDatabase();
        db.update("testinterface", v, null, null);

        Cursor c = db.query("testinterface", null, null, null, null, null, null);

        assertNotNull(c);
        assertEquals(1, c.getCount());
        assertTrue(c.moveToFirst());

        CursorGetter g = new CursorGetter(c);
        TestInterface t = new TestInterface(g);
        assertEquals(7, t.getLong());
        assertEquals("upgraded", t.getString());
        assertEquals("new string", t.newString);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        TestInterface.sDBVersion = 1;
    }
}
