package io.vokal.db;

import android.content.ContentValues;

public interface DataModelInterface {

    /*
     * table creation: add your model field columns and constraints using CreateBuilder
     */
    SQLiteTable onTableCreate(SQLiteTable.Builder aBuilder);

    /*
     * upgrades: re-create or add tables, index, seed data using UpgradeBuilder
     */
    SQLiteTable onTableUpgrade(SQLiteTable.Upgrader aUpgrader, int aOldVersion);

    /*
     * put values to be saved
     */
    void populateContentValues(ContentValues aValues);

}
