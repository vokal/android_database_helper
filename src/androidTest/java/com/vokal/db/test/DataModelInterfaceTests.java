package com.vokal.db.test;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.ProviderTestCase2;

import java.util.ArrayList;
import java.util.Date;

import com.vokal.db.*;
import com.vokal.db.test.models.*;
import com.vokal.db.util.ObjectCursor;

public class DataModelInterfaceTests extends ProviderTestCase2<SimpleContentProvider> {


    public DataModelInterfaceTests() {
        super(SimpleContentProvider.class, "com.vokal.database");
    }

    private Context mContext;
    private Cursor mCursor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getMockContext();
        DatabaseHelper.registerModel(mContext, ExtendedOne.class, ExtendedTwo.class, TestInterface.class);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
    }

    public void testInsert() {
        Date date = new Date();
        TestInterface testModel = new TestInterface();
        testModel.setString("test");
        testModel.setBoolean(false);
        testModel.setInteger(5);
        testModel.setLong(123123l);
        testModel.setFloat(123.123f);
        testModel.setDouble(2.3);
        testModel.setDouble(2.3);
        testModel.setDate(date);
        
        Uri uri = DatabaseHelper.save(mContext, testModel);
        assertNotNull(uri);

        mCursor = getMockContentResolver().query(DatabaseHelper.getContentUri(TestInterface.class),null,null,null,null);
        ObjectCursor<TestInterface> cursor = new ObjectCursor<TestInterface>(mCursor, TestInterface.CURSOR_CREATOR);
        if (cursor.moveToFirst()) {
            TestInterface m = cursor.getModel();
            assertEquals("test", m.getString());
            assertEquals(false, m.isBoolean());
            assertEquals(5, m.getInteger());
            assertEquals(123123l, m.getLong());
            assertEquals(2.3, m.getDouble());
            assertEquals(123.123f, m.getFloat());
            assertEquals(date, m.getDate());
        } else {
            assertFalse("cursor empty", true);
        }
    }

    /*public void testDelete() {
        TestInterface testModel = new TestInterface();
        testModel.setBoolean(false);
        testModel.setDouble(2.3);
        testModel.setString("test");
        testModel.setLong(123123l);
        Uri uri = DatabaseHelper.save(mContext, testModel);
        assertNotNull(uri);
        boolean success = testModel.delete(mContext);
        assertTrue(success);
    }*/

    public void testBulkInsert() {
        TestInterface testModel = new TestInterface();
        testModel.setInteger(1);
        testModel.setBoolean(false);
        testModel.setDouble(1.3);
        testModel.setString("tasdf");
        testModel.setLong(23123123l);
        testModel.setDate(new Date());

        TestInterface testModel2 = new TestInterface();
        testModel2.setInteger(2);
        testModel2.setBoolean(true);
        testModel2.setDouble(2.1);
        testModel2.setString("aaaa");
        testModel2.setLong(2312l);
        testModel2.setDate(new Date());

        TestInterface testModel3 = new TestInterface();
        testModel3.setInteger(3);
        testModel3.setBoolean(false);
        testModel3.setDouble(2.3);
        testModel3.setString("test");
        testModel3.setLong(123123l);
        testModel3.setDate(new Date());

        ArrayList<DataModelInterface> models = new ArrayList<DataModelInterface>();
        models.add(testModel);
        models.add(testModel2);
        models.add(testModel3);

        int count = DatabaseHelper.bulkInsert(mContext, models);
        assertEquals(count, 3);

    }

    public void testUpdate() {
        Date date = new Date();
        TestInterface testModel = new TestInterface();
        testModel.setBoolean(false);
        testModel.setInteger(5);
        testModel.setDouble(2.3);
        testModel.setString("test");
        testModel.setLong(123123l);
        testModel.setDate(date);
        Uri uri = DatabaseHelper.save(mContext, testModel);
        assertNotNull(uri);

        testModel.setString("updated");
        testModel.setBoolean(true);
        testModel.setDouble(4.1);
        date = new Date();
        testModel.setDate(date);

        uri = DatabaseHelper.save(mContext, testModel);
        assertNotNull(uri);

        long id = testModel.getLong();

        mCursor = getMockContentResolver().query(DatabaseHelper.getContentUri(TestInterface.class),null,null,null,null);
        ObjectCursor<TestInterface> cursor = new ObjectCursor<TestInterface>(mCursor, TestInterface.CURSOR_CREATOR);
        if (cursor.moveToFirst()) {
            TestInterface m = cursor.getModel();
            assertEquals("updated", m.getString());
            assertEquals(true, m.isBoolean());
            assertEquals(4.1, m.getDouble());
            assertEquals(id, m.getLong());
            assertEquals(date, m.getDate());
        } else {
            assertFalse("cursor empty", true);
        }
    }

    public void testWipeDatabase() {
        Date date = new Date();
        TestInterface testModel = new TestInterface();
        testModel.setBoolean(false);
        testModel.setDouble(2.3);
        testModel.setString("test");
        testModel.setLong(123123l);
        testModel.setDate(date);
        Uri uri = DatabaseHelper.save(mContext, testModel);

        ExtendedTwo extendedTwo = new ExtendedTwo();
        extendedTwo.setBoolean1(true);
        extendedTwo.setDouble1(3.4);
        extendedTwo.setString1("test2");
        extendedTwo.setLong1(555444333);
        extendedTwo.save(mContext);

        mCursor = getMockContentResolver().query(DatabaseHelper.getContentUri(TestInterface.class),null,null,null,null);
        ObjectCursor<TestInterface> cursor = new ObjectCursor<TestInterface>(mCursor, TestInterface.CURSOR_CREATOR);
        if (cursor.moveToFirst()) {
            TestInterface m = cursor.getModel();
            assertEquals(false, m.isBoolean());
            assertEquals(2.3, m.getDouble());
            assertEquals("test", m.getString());
            assertEquals(123123l, m.getLong());
            assertEquals(date, m.getDate());
        } else {
            assertFalse("cursor empty", true);
        }
        mCursor.close();

        mCursor = getMockContentResolver().query(DatabaseHelper.getContentUri(ExtendedTwo.class),null,null,null,null);
        ObjectCursor<ExtendedTwo> cursor2 = new ObjectCursor<ExtendedTwo>(mCursor, ExtendedTwo.CURSOR_CREATOR);
        if (cursor2.moveToFirst()) {
            ExtendedTwo m = cursor2.getModel();
            assertEquals(true, m.isBoolean1());
            assertEquals(3.4, m.getDouble1());
            assertEquals("test2", m.getString1());
            assertEquals(555444333, m.getLong1());
        } else {
            assertFalse("cursor empty", true);
        }
        mCursor.close();

        DatabaseHelper.wipeDatabase(mContext);

        mCursor = getMockContentResolver().query(DatabaseHelper.getContentUri(TestInterface.class),null,null,null,null);
        cursor = new ObjectCursor<TestInterface>(mCursor, TestInterface.CURSOR_CREATOR);
        assertFalse("rows found", cursor.moveToFirst());
        mCursor.close();

        mCursor = getMockContentResolver().query(DatabaseHelper.getContentUri(ExtendedTwo.class),null,null,null,null);
        cursor2 = new ObjectCursor<ExtendedTwo>(mCursor, ExtendedTwo.CURSOR_CREATOR);
        assertEquals(cursor2.moveToFirst(), false);
        mCursor.close();

    }

}
