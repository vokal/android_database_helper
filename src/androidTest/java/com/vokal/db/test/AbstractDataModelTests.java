package com.vokal.db.test;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.test.ProviderTestCase2;

import com.vokal.db.*;
import com.vokal.db.test.models.*;
import com.vokal.db.util.ObjectCursor;

import java.util.ArrayList;
import java.util.Date;

public class AbstractDataModelTests extends ProviderTestCase2<SimpleContentProvider> {

    private Context mContext;

    public AbstractDataModelTests() {
        super(SimpleContentProvider.class, "com.vokal.database");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getMockContext();
        DatabaseHelper.registerModel(mContext, ExtendedOne.class, ExtendedTwo.class, TestInterface.class);
    }

    public void testInsert() {
        ExtendedOne testModel = new ExtendedOne();
        testModel.setBoolean1(false);
        testModel.setDouble1(2.3);
        testModel.setString1("test");
        testModel.setLong1(123123l);
        Uri uri = testModel.save(mContext);
        assertNotNull(uri);

        long id = testModel.getId();

        Cursor c = getMockContentResolver().query(DatabaseHelper.getContentUri(ExtendedOne.class),null,null,null,null);
        ObjectCursor<ExtendedOne> cursor = new ObjectCursor<ExtendedOne>(c, ExtendedOne.CURSOR_CREATOR);
        if (cursor.moveToFirst()) {
            ExtendedOne m = cursor.getModel();
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
        ExtendedOne testModel = new ExtendedOne();
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
        ExtendedOne testModel = new ExtendedOne();
        testModel.setBoolean1(false);
        testModel.setDouble1(1.3);
        testModel.setString1("tasdf");
        testModel.setLong1(23123123l);

        ExtendedOne testModel2 = new ExtendedOne();
        testModel2.setBoolean1(true);
        testModel2.setDouble1(2.1);
        testModel2.setString1("aaaa");
        testModel2.setLong1(2312l);

        ExtendedOne testModel3 = new ExtendedOne();
        testModel3.setBoolean1(false);
        testModel3.setDouble1(2.3);
        testModel3.setString1("test");
        testModel3.setLong1(123123l);

        ArrayList<AbstractDataModel> models = new ArrayList<AbstractDataModel>();
        models.add(testModel);
        models.add(testModel2);
        models.add(testModel3);

        int count = ExtendedOne.bulkInsert(mContext, models);
        assertEquals(count, 3);

    }

    public void testUpdate() {
        ExtendedOne testModel = new ExtendedOne();
        testModel.setBoolean1(false);
        testModel.setDouble1(2.3);
        testModel.setString1("test");
        testModel.setLong1(123123l);
        Uri uri = testModel.save(mContext);
        final long id = testModel.getId();
        assertNotNull(uri);

        testModel.setBoolean1(true);
        testModel.setDouble1(4.1);

        uri = testModel.save(mContext);
        assertNotNull(uri);

        assertEquals(id, testModel.getId());

        Cursor c = getMockContentResolver().query(DatabaseHelper.getContentUri(ExtendedOne.class),null,null,null,null);
        ObjectCursor<ExtendedOne> cursor = new ObjectCursor<ExtendedOne>(c, ExtendedOne.CURSOR_CREATOR);
        if (cursor.moveToFirst()) {
            ExtendedOne m = cursor.getModel();
            assertEquals(true, m.isBoolean1());
            assertEquals(4.1, m.getDouble1());
            assertEquals(id, m.getId());
        } else {
            assertFalse("cursor empty", true);
        }


    }

    public void testWipeDatabase() {
        ExtendedOne testModel = new ExtendedOne();
        testModel.setBoolean1(false);
        testModel.setDouble1(2.3);
        testModel.setString1("test");
        testModel.setLong1(123123l);
        testModel.save(mContext);

        ExtendedTwo extendedTwo = new ExtendedTwo();
        extendedTwo.setBoolean1(true);
        extendedTwo.setDouble1(3.4);
        extendedTwo.setString1("test2");
        extendedTwo.setLong1(555444333);
        extendedTwo.save(mContext);

        Cursor c = getMockContentResolver().query(DatabaseHelper.getContentUri(ExtendedOne.class),null,null,null,null);
        ObjectCursor<ExtendedOne> cursor = new ObjectCursor<ExtendedOne>(c, ExtendedOne.CURSOR_CREATOR);
        if (cursor.moveToFirst()) {
            ExtendedOne m = cursor.getModel();
            assertEquals(false, m.isBoolean1());
            assertEquals(2.3, m.getDouble1());
            assertEquals("test", m.getString1());
            assertEquals(123123l, m.getLong1());
        } else {
            assertFalse("cursor empty", true);
        }

        c = getMockContentResolver().query(DatabaseHelper.getContentUri(ExtendedTwo.class),null,null,null,null);
        ObjectCursor<ExtendedTwo> cursor2 = new ObjectCursor<ExtendedTwo>(c, ExtendedTwo.CURSOR_CREATOR);
        if (cursor2.moveToFirst()) {
            ExtendedTwo m = cursor2.getModel();
            assertEquals(true, m.isBoolean1());
            assertEquals(3.4, m.getDouble1());
            assertEquals("test2", m.getString1());
            assertEquals(555444333, m.getLong1());
        } else {
            assertFalse("cursor empty", true);
        }

        DatabaseHelper.wipeDatabase(mContext);

        c = getMockContentResolver().query(DatabaseHelper.getContentUri(ExtendedOne.class),null,null,null,null);
        cursor = new ObjectCursor<ExtendedOne>(c, ExtendedOne.CURSOR_CREATOR);
        assertEquals(cursor.moveToFirst(), false);

        c = getMockContentResolver().query(DatabaseHelper.getContentUri(ExtendedTwo.class),null,null,null,null);
        cursor2 = new ObjectCursor<ExtendedTwo>(c, ExtendedTwo.CURSOR_CREATOR);
        assertEquals(cursor2.moveToFirst(), false);


    }

    public void testAutoIncrement() {
        ExtendedOne testModel = new ExtendedOne();
        testModel.setBoolean1(false);
        testModel.setDouble1(2.3);
        testModel.setString1("test");
        testModel.setLong1(123123l);
        testModel.save(mContext);

        long id = testModel.getId();

        ExtendedOne test2Model = new ExtendedOne();
        test2Model.setBoolean1(true);
        test2Model.setDouble1(3.4);
        test2Model.setString1("test2");
        test2Model.setLong1(555444333);
        test2Model.save(mContext);

        long id2 = test2Model.getId();

        assertEquals(id+1, id2);

    }

    public void testUniqueness() {
        ExtendedTwo testModel = new ExtendedTwo();
        testModel.setBoolean1(false);
        testModel.setDouble1(2.3);
        testModel.setString1("test");
        testModel.setLong1(123123l);
        testModel.setInt1(12);
        testModel.save(mContext);

        assertEquals(testModel.getInt1(), 12);

        Cursor c = getMockContentResolver().query(DatabaseHelper.getContentUri(ExtendedTwo.class),null,null,null,null);
        assertTrue(c.moveToFirst());
        assertEquals(c.getCount(), 1);

        ExtendedTwo extendedTwo = new ExtendedTwo();
        extendedTwo.setBoolean1(true);
        extendedTwo.setDouble1(3.4);
        extendedTwo.setString1("test2");
        extendedTwo.setLong1(555444333);
        extendedTwo.setInt1(12);
        extendedTwo.save(mContext);
        assertEquals(extendedTwo.getInt1(), 12);

        Cursor c2 = getMockContentResolver().query(DatabaseHelper.getContentUri(ExtendedTwo.class),null,null,null,null);
        ObjectCursor<ExtendedTwo> cursor2 = new ObjectCursor<>(c2, ExtendedTwo.CURSOR_CREATOR);
        assertTrue(c2.moveToFirst());
        assertEquals(c2.getCount(), 1);
        assertEquals(cursor2.getModel().getDouble1(), 3.4);
    }

    public void testParcelable() {
        //Create parcelable object and put to Bundle

        final long id = 123L;

        ExtendedTwo extendedTwo = new ExtendedTwo();
        extendedTwo.setId(id);
        extendedTwo.setString1("parcelled");
        extendedTwo.setBoolean1(true);
        extendedTwo.setInt1(12);
        extendedTwo.setLong1(555444333);
        extendedTwo.setFloat1(123.456f);
        extendedTwo.setDouble1(3.4);
        extendedTwo.setDate1(new Date());

        extendedTwo.save(mContext);
        assertEquals(extendedTwo.getInt1(), 12);
        Bundle b = new Bundle();
        b.putParcelable("e2", extendedTwo);

        //Save bundle to parcel
        Parcel parcel = Parcel.obtain();
        b.writeToParcel(parcel, 0);

        //Extract bundle from parcel
        parcel.setDataPosition(0);
        Bundle b2 = parcel.readBundle();
        b2.setClassLoader(ExtendedTwo.class.getClassLoader());
        ExtendedTwo e2 = b2.getParcelable("e2");

        //Check that objects are not same and test that objects are equal
        assertFalse("Bundle is the same", b2 == b);
        assertFalse("model is the same", e2 == extendedTwo);
        assertEquals(id, e2.getId());
        assertEquals("parcelled", e2.getString1());
        assertTrue(e2.isBoolean1());
        assertEquals(extendedTwo.getInt1(), e2.getInt1());
        assertEquals(extendedTwo.getLong1(), e2.getLong1());
        assertEquals(extendedTwo.getFloat1(), e2.getFloat1());
        assertEquals(extendedTwo.getDouble1(), e2.getDouble1());
        assertEquals(extendedTwo.getDate1(), e2.getDate1());
    }
}
