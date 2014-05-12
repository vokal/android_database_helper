package com.vokal.db.util;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.*;


@SuppressWarnings("unchecked")
public abstract class ObjectCursorAdapter<T> extends CursorAdapter {

    public ObjectCursorAdapter(Context aContext, ObjectCursor<T> aObjectCursor) {
        super(aContext, aObjectCursor, 0);
    }

    @Override
    public final View newView(Context aContext, Cursor aCursor, ViewGroup aParent) {
        return newView(aContext, LayoutInflater.from(aContext), aParent, ((ObjectCursor<T>) aCursor).getModel());
    }

    public abstract View newView(Context aContext, LayoutInflater aInflater, ViewGroup aParent, T aObject);

    @Override
    public final void bindView(View aView, Context aContext, Cursor aCursor) {
        bindView(aContext, aView, ((ObjectCursor<T>) aCursor).getModel());
    }

    public abstract void bindView(Context aContext, View aView, T aObject);

    public T getItem(int position) {
        if (mCursor.getPosition() == position) {
            return ((ObjectCursor<T>) mCursor).getModel();
        } else if (position < mCursor.getCount() - 1) {
            mCursor.moveToPosition(position);
            return ((ObjectCursor<T>) mCursor).getModel();
        }
        return null;
    }
}
