#ADHD - Android Database Helper Dummy
---

Whenever you need to persist anything more than a small amount of data you will need a few things right at the start to even get going:

1. SQLiteOpenHelper
2. ContentProvider
3. Data Models

This library serves as a drop-in starter for the helper and provider plus builders for defining the data models. 
The ultimate goal is to eliminate the boilerplate necessary to create database backed models and reduce and simplify overhead in the model classes. 

##Usage
---
Add the provider tag to your app manifest:

	<provider
    	android:name="com.vokal.database.SimpleContentProvider"
        android:authorities="com.example.database.authority"
        android:exported="false"
        />
        
Add optional metadata for name and version to provider:

	<provider ... >
        <meta-data android:name="database_name" android:value="my_database.db" />
        <meta-data android:name="database_version" android:value="1" />
	</provider>
	       
Implement a concrete `AbstractDataModel` class:

	public class User extends AbstractDataModel
	
		public static final String COL_ID = BaseColumns._ID;
		public static final String COL_NAME = "name";
		
		private long id;
		private String name;
		
		...
	
		protected void populateContentValues(ContentValues aValues) {
			// put fields in aValues

			aValues.put(COL_ID, id);
			aValues.put(COL_NAME, name);
			
		}
		
`AbstractDataModel` provides member methods:

	Uri getContentUri();          // to get this model's content Uri
	Uir save(Context aContext);   // to save this modle (calls populateContentValues)
	
Add static SQLiteTable.TableCreator constant named TABLE_CREATOR to model class:


	public class User extends AbstractData Model {
	
		...
		
		public static final SQLiteTable.TableCreator TABLE_CREATOR = new SQLiteTable.Creator() {
	
			@Override
    		protected SQLiteTable buildTableSchema(SQLiteTable.Builder aBuilder) {
        		// use aBuilder methods to add columns
       			//  - addStringColumn, addIntegerColumn, etc..
        		//  - call primaryKey(), autoincrement(), unique() after adding a column
        
        		aBuilder.addIntegerColumn(COL_ID).primaryKey().autoincrement()
                    	.addStringColumn(COL_NAME).unique();
                    
        		return aBuilder.build();
    		}
    		
    		@Override
        	public SQLiteTable updateTableSchema(SQLiteTable.Updater aUpdater, int aOldVersion) {
            	return null;  // no upgrades yet...
        }   
     
	  		      
Register your model with DatabaseHelper and save your new content Uri:

	public class MyApp extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();

		DatabaseHelper.registerModel(this, User.class, MyModel.class);
		DatabaseHelper.registerModel(this, SomeModel.class, "some_table_name")
		
	}

After registering models you can get the content Uri via the DatabaseHelper:

	Uri userContentUri = DatabaseHelper.getContentUri(User.class);
	
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
	
###Joins:
	
You can request a joined table content Uri:
	
	Uri userTransactionsUri = DatabaseHelper.getJoindedContentUri(Transaction.class, Transaction.COL_USER_ID, User.class, User.COL_ID);

This would equates to (assuming table and column names):
	
	SELECT ... FROM transaction LEFT OUTER JOIN user ON (transaction.user_id = user._id) WHERE ...
		
__NOTE: this feature is in development and considered preview__  

*column names in resulting cursors are not prefixed with table name so you could end up with multiple columns with the same name*
	

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

###SQLiteTable.Updater:

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
	

	
