android_database_helper
---

Whenever you need to persist anything more than a small amount of data you will need a few things right at the start to even get going:

1. SQLiteOpenHelper
2. ContentProvider
3. Data Models

This library serves as a drop-in starter for the helper and provider plus builders for defining the data models.

The ultimate goal is to eliminate the boilerplate necessary to create database backed models and reduce and simplify overhead in the model classes. 