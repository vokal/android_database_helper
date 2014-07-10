package com.vokal.db.test;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.ProviderTestCase2;

import com.vokal.db.*;
import com.vokal.db.util.ObjectCursor;

import java.util.ArrayList;

public class DataModelTest extends ProviderTestCase2<SimpleContentProvider> {

    private Context mContext;

    public DataModelTest() {
        super(SimpleContentProvider.class,"com.vokal.database");

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getMockContext();
        DatabaseHelper.registerModel(mContext, TestModel.class, Test2Model.class);
    }

    public void testInsert() {
        TestModel testModel = new TestModel();
        testModel.setBoolean1(false);
        testModel.setDouble1(2.3);
        testModel.setString1("test");
        testModel.setLong1(123123l);
        Uri uri = testModel.save(mContext);
        assertNotNull(uri);

        long id = testModel.getId();

        Cursor c = getMockContentResolver().query(DatabaseHelper.getContentUri(TestModel.class),null,null,null,null);
        ObjectCursor<TestModel> cursor = new ObjectCursor<TestModel>(c, TestModel.CURSOR_CREATOR);
        if (cursor.moveToFirst()) {
            TestModel m = cursor.getModel();
            assertEquals(false, m.isBoolean1());
            assertEquals(2.3, m.getDouble1());
            assertEquals("test", m.getString1());
            assertEquals(123123l, m.getLong1());
            assertEquals(id, m.getId());
        } else {
            assertFalse("cursor empty", true);
        }
    }

    public void testDelete() {
        TestModel testModel = new TestModel();
        testModel.setBoolean1(false);
        testModel.setDouble1(2.3);
        testModel.setString1("test");
        testModel.setLong1(123123l);
        Uri uri = testModel.save(mContext);
        assertNotNull(uri);
        boolean success = testModel.delete(mContext);
        assertTrue(success);
    }

    public void testBulkInsert() {
        TestModel testModel = new TestModel();
        testModel.setBoolean1(false);
        testModel.setDouble1(1.3);
        testModel.setString1("tasdf");
        testModel.setLong1(23123123l);

        TestModel testModel2 = new TestModel();
        testModel.setBoolean1(true);
        testModel.setDouble1(2.1);
        testModel.setString1("aaaa");
        testModel.setLong1(2312l);

        TestModel testModel3 = new TestModel();
        testModel.setBoolean1(false);
        testModel.setDouble1(2.3);
        testModel.setString1("test");
        testModel.setLong1(123123l);

        ArrayList<AbstractDataModel> models = new ArrayList<AbstractDataModel>();
        models.add(testModel);
        models.add(testModel2);
        models.add(testModel3);

        int count = TestModel.bulkInsert(mContext, models);
        assertEquals(count, 3);

    }

    public void testUpdate() {
        TestModel testModel = new TestModel();
        testModel.setBoolean1(false);
        testModel.setDouble1(2.3);
        testModel.setString1("test");
        testModel.setLong1(123123l);
        Uri uri = testModel.save(mContext);
        assertNotNull(uri);

        testModel.setBoolean1(true);
        testModel.setDouble1(4.1);

        uri = testModel.save(mContext);
        assertNotNull(uri);

        long id = testModel.getId();

        Cursor c = getMockContentResolver().query(DatabaseHelper.getContentUri(TestModel.class),null,null,null,null);
        ObjectCursor<TestModel> cursor = new ObjectCursor<TestModel>(c, TestModel.CURSOR_CREATOR);
        if (cursor.moveToFirst()) {
            TestModel m = cursor.getModel();
            assertEquals(true, m.isBoolean1());
            assertEquals(4.1, m.getDouble1());
            assertEquals(id, m.getId());
        } else {
            assertFalse("cursor empty", true);
        }


    }

    public void testWipeDatabase() {
        TestModel testModel = new TestModel();
        testModel.setBoolean1(false);
        testModel.setDouble1(2.3);
        testModel.setString1("test");
        testModel.setLong1(123123l);
        testModel.save(mContext);

        Test2Model test2Model = new Test2Model();
        test2Model.setBoolean1(true);
        test2Model.setDouble1(3.4);
        test2Model.setString1("test2");
        test2Model.setLong1(555444333);
        test2Model.save(mContext);

        Cursor c = getMockContentResolver().query(DatabaseHelper.getContentUri(TestModel.class),null,null,null,null);
        ObjectCursor<TestModel> cursor = new ObjectCursor<TestModel>(c, TestModel.CURSOR_CREATOR);
        if (cursor.moveToFirst()) {
            TestModel m = cursor.getModel();
            assertEquals(false, m.isBoolean1());
            assertEquals(2.3, m.getDouble1());
            assertEquals("test", m.getString1());
            assertEquals(123123l, m.getLong1());
        } else {
            assertFalse("cursor empty", true);
        }

        c = getMockContentResolver().query(DatabaseHelper.getContentUri(Test2Model.class),null,null,null,null);
        ObjectCursor<Test2Model> cursor2 = new ObjectCursor<Test2Model>(c, Test2Model.CURSOR_CREATOR);
        if (cursor2.moveToFirst()) {
            Test2Model m = cursor2.getModel();
            assertEquals(true, m.isBoolean1());
            assertEquals(3.4, m.getDouble1());
            assertEquals("test2", m.getString1());
            assertEquals(555444333, m.getLong1());
        } else {
            assertFalse("cursor empty", true);
        }

        DatabaseHelper.wipeDatabase(mContext);

        c = getMockContentResolver().query(DatabaseHelper.getContentUri(TestModel.class),null,null,null,null);
        cursor = new ObjectCursor<TestModel>(c, TestModel.CURSOR_CREATOR);
        assertEquals(cursor.moveToFirst(), false);

        c = getMockContentResolver().query(DatabaseHelper.getContentUri(Test2Model.class),null,null,null,null);
        cursor2 = new ObjectCursor<Test2Model>(c, Test2Model.CURSOR_CREATOR);
        assertEquals(cursor2.moveToFirst(), false);


    }

    public void testAutoIncrement() {
        TestModel testModel = new TestModel();
        testModel.setBoolean1(false);
        testModel.setDouble1(2.3);
        testModel.setString1("test");
        testModel.setLong1(123123l);
        testModel.save(mContext);

        long id = testModel.getId();

        TestModel test2Model = new TestModel();
        test2Model.setBoolean1(true);
        test2Model.setDouble1(3.4);
        test2Model.setString1("test2");
        test2Model.setLong1(555444333);
        test2Model.save(mContext);

        long id2 = test2Model.getId();

        assertEquals(id+1, id2);

    }

}
