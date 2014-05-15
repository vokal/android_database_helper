package com.vokal.db.test;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.ProviderTestCase2;

import com.vokal.db.AbstractDataModel;
import com.vokal.db.util.CursorGetter;
import com.vokal.db.util.ObjectCursor;
import com.vokal.db.DatabaseHelper;
import com.vokal.db.SimpleContentProvider;

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
        DatabaseHelper.registerModel(mContext, TestModel.class, "test_model");
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

}
