[![Stories in Ready](https://badge.waffle.io/vokalinteractive/android_database_helper.png?label=ready&title=Ready)](https://waffle.io/vokalinteractive/android_database_helper)
#ADHD - Android Database Helper Dummy
---

Whenever you need to persist anything more than a small amount of data you will need a few things right at the start to even get going:

1. SQLiteOpenHelper
2. ContentProvider
3. Data Models

This library serves as a drop-in starter for the helper and provider plus classes and interfaces for defining the data models. 
The ultimate goal is to reduce the boilerplate for database backed models so you can focus on using them faster. 

Usage
---
Add the provider tag to your app manifest:

````xml
	<provider
    	android:name="com.vokal.database.SimpleContentProvider"
    	android:authorities="com.example.database.authority"
    	android:exported="false"
    	/>
````
        
Add optional metadata for name and version to provider:

````xml
	<provider ... >
    	<meta-data android:name="database_name" android:value="my_database.db" />
    	<meta-data android:name="database_version" android:value="1" />
	</provider>
````

There are several ways to create a data model class:

 - extending `AbstractDataModel` is the quickest as it provides convenience methods (ie. save(), delete()), and it's a Parcelable object (see below)
 - implementing `DataModelInterface` gives you the easy table setup methods and the populateContentValues() method.  DatabaseHelper has static methods for save/update and bulk insert, but you will have to handle deletes manually.
 - adding a `TableCreator TABLE_CREATOR` constant to your model class if you cannot do either of the above.  Tables will be created/upgraded, but you will have to handle all other operations manually.
  		      
Register your model with DatabaseHelper and save your new content Uri:

	public class MyApp extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();

		DatabaseHelper.registerModel(this, User.class, MyModel.class);
		DatabaseHelper.registerModel(this, SomeModel.class, "some_table_name")
		
	}

After registering models you can get the content Uri via the DatabaseHelper or from a AbstractDataModel instance:

	Uri userContentUri = DatabaseHelper.getContentUri(User.class);
	
	User user = new User();
	Uri uri = user.getContentUri();
	
Once the AbstractDataModel has been saved, you can get the content item Uri:

	user.save();
	Uri uri = user.getContentItemUri();


###`AbstractDataModel implements DataModelInterface` :
		
AbstractDataModel provides additional convenience member methods:

	Uri getContentUri();          		// to get this model's main content Uri
	Uri getContentItemUri();      		// get the item Uri or null if it hasn't been saved
	Uri save(Context aContext);   	 	// to save this model (calls populateContentValues)
	boolean delete(Context aContext);	// to delete this model
	

Override `onCreateTable(..)` and `onUpgradeTable(...)` to define the column schema. Override `populateContentValues(...)` to set your model fields for saves/updates:

````java

	public class User extends AbstractDataModel {

    	public static final String COL_NAME = "name";

    	private String name;
    	
    	public SQLiteTable onTableCreate(SQLiteTable.Builder aBuilder) {
    		return aBuilder
    				.addStringColumn(COL_NAME).unique()
                	.build();
        }
        
        // do not override or return null to do nothing on upgrade
        public SQLiteTable onTableUpgrade(SQLiteTable.Updater aUpdater, int aOldVersion) {
        	return aBuilder.recreate().build(); // will drop and re-create table
        }
    

    		@Override
    	public void populateContentValues(ContentValues aValues) {
            super.populateContentValues(aValues);
            
            // put fields in aValues
            aValues.put(COL_NAME, name);
    	}
````

Optionally make `AbstractDataModel` pass model fields via Parcelable overriding the Parcel constructor and writeToParcel:

````java
    
    MyModel(Parcel bundle) {
        super(bundle);

        // read from bundle
    }

    @Override
    public void writeToParcel(Parcel bundle, int flags) {
        super.writeToParcel(bundle, flags);
        
        // write to bundle
    }

````


###`SQLiteTable.TableCreator TABLE_CREATOR`

For when you can't extend or implement your own data model class

````java

	public class User {

    	...

    	public static final SQLiteTable.TableCreator TABLE_CREATOR = new SQLiteTable.Creator() {

        	@Override
        	protected SQLiteTable buildTableSchema(SQLiteTable.Builder aBuilder) {
            	// use aBuilder methods to add columns
            	//  - addStringColumn, addIntegerColumn, etc..
            	//  - call primaryKey(), autoincrement(), unique() after adding a column

            	aBuilder.addIntegerColumn(COL_ID).primaryKey().autoincrement()
                	.addStringColumn(COL_NAME).unique()

            	return aBuilder.build();
        	}

        	@Override
        	public SQLiteTable updateTableSchema(SQLiteTable.Updater aUpdater, int aOldVersion) {
            	return null;  // does nothing on upgrade...
        	}
    	}
	}   
	
````
	
###Upgrades:

Increment version number in provider meta:

	<provider ... >
        <meta-data android:name="database_version" android:value="3" />
	</provider>


Implement the updateTableSchema using Updater and add to provideContentValues:  

	public class User extends AbstractData Model {
	
		...
		public static final String COL_EMAIL = "email"; // added in db version 2
		public static final String COL_PHONE = "phone"; // added in db version 3
		
		...
		public String email;
		public String phone;
		
		public static final SQLiteTable.TableCreator TABLE_CREATOR = new SQLiteTable.Creator() {
			...
			
			@Override
	        public SQLiteTable updateTableSchema(SQLiteTable.Updater aUpdater, int aOldVersion) {
				switch (aOldVersion) {
        	   		case 2:
		    	       	aUpdater.addStringColumn(COL_EMAIL)
    	        	case 3: 
    	           		aUpdate.addStringColumn(COL_PHONE);
				}
				return aUpdater.build();
			}
		}
		
		protected void populateContentValues(ContentValues aValues) {
			...
			aValues.put(COL_EMAIL, email);
			aValues.put(COL_PHONE, phone);
		}
	}
	
Returning `aUpgrader.recreate().build()` will cause the table to be dropped and recreated on upgrades.  Returning `null` causes nothing to happen on upgrade (ie. schema hasn't changed).
	
###Joins:
	
You can request a joined table content Uri:
	
	Uri userTransactionsUri = DatabaseHelper.getJoindedContentUri(Transaction.class, Transaction.COL_USER_ID, User.class, User.COL_ID);

This would equates to (assuming table and column names):
	
	SELECT ... FROM transaction LEFT OUTER JOIN user ON (transaction.user_id = user._id) WHERE ...
	
A default projection map will be generated that maps `table.column` to `table_column` to avoid Android issues with accessing the fields.  You can override this behavior by providing your own projection map with:

	DatabaseHelper.setProjectionMap(Uri aContentUri, Map<String, String> aProjectionMap);
	
`CursorGetter` also provides a `setTable()` method for accessign these table prefixed fields.

##Builder/Updater Methods
---
Call column constraints immediately after adding a column.  Table constraints and extras can be called in any order.

###SQLiteTable.Builder:

####Column Adders:
	addStringColumn(String)
	addIntegerColumn(String) // for int or long
	addRealColumn(String)    // for float or double
	addBlobColumn(String)    // for byte[]
	addNullColumn(String)
	
####Column Constraints:
	primaryKey()  // cannot use if primaryKey(String...) table constraint is used
	autoincrement()
	unique()
	nonNull()
	defaultValue(String)
	defaultValue(long)
	defaultValue(double)
	defaultCurrentTime()
	defaultCurrentDate()
	defaultCurrentTimestamp()

####Table Constraints:
	primaryKey(String...) // cannot have called primaryKey() column constraint
	unique(String...)
	index(String...)
	nullHack(String)

####Extras:
	seed(ContentValues...)

###SQLiteTable.Upgrader:

####Column Adders:
	addStringColumn(String)
	addIntegerColumn(String) // for int or long
	addRealColumn(String)    // for float or double
	addBlobColumn(String)    // for byte[]
	addNullColumn(String)

####Column Constraints:
	nonNull()
	defaultValue(String)
	defaultValue(long)
	defaultValue(double)

####Table Constraints:
	index(String...)
	nullHack(String)

####Extras:
	seed(ContentValues...)
	recreate() // for drop table and recreate using Builder
	

	
