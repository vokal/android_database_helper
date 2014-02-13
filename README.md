##android_database_helper
---

Whenever you need to persist anything more than a small amount of data you will need a few things right at the start to even get going:

1. SQLiteOpenHelper
2. ContentProvider
3. Data Models

This library serves as a drop-in starter for the helper and provider plus builders for defining the data models.

The ultimate goal is to eliminate the boilerplate necessary to create database backed models and reduce and simplify overhead in the model classes. 

###Usage:
Add the provider tag to your app manifest:

	<provider
    	android:name="com.vokal.database.SimpleContentProvider"
        android:authorities="com.vokal.database"
        android:exported="false"
        />
                
Implement a concrete `AbstractDataModel` class:

	public class MyModel extends AbstractDataModel
	
		@Override
    	protected SQLiteTable buildTableSchema(SQLiteTable.Builder aBuilder) {
        	// use aBuilder methods to add columns
       		//  - addStringColumn, addIntegerColumn, etc..
        	//  - call primaryKey(), autoincrement(), unique() after adding a column
        	// 
        	// (optionally: just return your own SQLiteTable if Builder is insufficient)
        
        	aBuilder.addIntegerColumn("_ID").primaryKey().autoincrement()
                    .addStringColumn("name").unique();
                    
        	return aBuilder.build();
    	}
    
    	@Override
    	protected Object getValueOfColumn(String aColumnName) {
    		// return the member variable corresponding to the column name given
    		
        	switch (aColumnName) {
            	case "_ID": return mId;
            	case "name": return mName;
        	}
        	return null;
    	}
  	}
    
Register your model with DatabaseHelper and save your new content Uri:

	public static final Uri CONTENT_URI = DatabaseHelper.registerModel(MyModel.class);

