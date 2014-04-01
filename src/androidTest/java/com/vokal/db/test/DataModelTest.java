package com.vokal.db.test;

import android.app.Instrumentation;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.InstrumentationTestCase;
import android.test.ProviderTestCase2;

import com.vokal.db.CursorGetter;
import com.vokal.db.DatabaseHelper;
import com.vokal.db.SimpleContentProvider;

public class DataModelTest extends ProviderTestCase2<SimpleContentProvider> {

    private Context mContext;

    public DataModelTest() {
        super(SimpleContentProvider.class,"com.vokal.database");

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getMockContext();
        DatabaseHelper.registerModel(mContext, TestModel.class, "test_model");
    }

    public void testInsert() {
        TestModel testModel = new TestModel();
        testModel.setBoolean1(false);
        testModel.setDouble1(2.3);
        testModel.setInt1(3);
        testModel.setString1("test");
        testModel.setLong1(123123l);
        Uri uri = testModel.save(mContext);

        Cursor c = getMockContentResolver().query(DatabaseHelper.getContentUri(TestModel.class),null,null,null,null);
        if (c.moveToFirst()) {
            CursorGetter getter = new CursorGetter(c);
            assertEquals(false, getter.getInt((TestModel.COL_BOOLEAN)) == 1);
            assertEquals(3, getter.getInt(TestModel.COL_INT));
            assertEquals(2.3, getter.getDouble(TestModel.COL_DOUBLE));
            assertEquals("test", getter.getString(TestModel.COL_STRING));
            assertEquals(123123l, getter.getInt(TestModel.COL_LONG));
        } else {
            assertFalse("cursor empty", true);
        }
    }
}
