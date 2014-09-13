package com.vokal.db;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import java.util.List;

import com.vokal.db.util.CursorGetter;

import static com.vokal.db.SQLiteTable.Builder;

public interface DataModelInterface {

    /*
     * table creation: add your model field columns and constraints using CreateBuilder
     */
    SQLiteTable onTableCreate(Builder aBuilder);

    /*
     * upgrades: re-create or add tables, index, seed data using UpgradeBuilder
     */
    SQLiteTable onTableUpgrade(SQLiteTable.Upgrader aUpgrader, int aOldVersion);

    /*
     * put values to be saved
     */
    void populateContentValues(ContentValues aValues);

}
