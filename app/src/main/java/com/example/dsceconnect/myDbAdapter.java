package com.example.dsceconnect;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Message;


public class myDbAdapter {
    myDbHelper myhelper;
    public myDbAdapter(Context context)
    {
        myhelper = new myDbHelper(context);
    }


    public long insertData(String enckey, String encid)
    {
        SQLiteDatabase dbb = myhelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(myDbHelper.ENCKEY, enckey);
        contentValues.put(myDbHelper.ENCID, encid);
        long id = dbb.insert(myDbHelper.TABLE_NAME, null , contentValues);
        return id;
    }


    public long getRowCount()
    {
        SQLiteDatabase db = myhelper.getWritableDatabase();
        String[] columns = {myDbHelper.ENCKEY,myDbHelper.ENCID};
        Cursor cursor =db.query(myDbHelper.TABLE_NAME,columns,null,null,null,null,null);
        long count=cursor.getCount();
        return count;
    }

    public String getEncData()
    {
        String finalString=null;

        SQLiteDatabase db = myhelper.getWritableDatabase();
        String[] columns = {myDbHelper.ENCKEY,myDbHelper.ENCID};
        Cursor cursor =db.query(myDbHelper.TABLE_NAME,columns,null,null,null,null,null);
        int count=1;

        while (cursor.moveToNext()&&count==1)
        {
            String enckey =cursor.getString(cursor.getColumnIndex(myDbHelper.ENCKEY));
            String  encid =cursor.getString(cursor.getColumnIndex(myDbHelper.ENCID));
            finalString=enckey+" "+encid;
            count++;
        }
        return finalString;
    }

    public int deleteAll()
    {
        SQLiteDatabase db = myhelper.getWritableDatabase();
        int count=db.delete(myDbHelper.TABLE_NAME,null,null);
        return count;
    }

    public  int delete(String enckey)
    {
        SQLiteDatabase db = myhelper.getWritableDatabase();
        String[] whereArgs ={enckey};

        int count =db.delete(myDbHelper.TABLE_NAME ,myDbHelper.ENCKEY+" = ?",whereArgs);
        return  count;
    }



    static class myDbHelper extends SQLiteOpenHelper
    {
        private static final String DATABASE_NAME = "myDatabase";    // Database Name
        private static final String TABLE_NAME = "USERTABLE";   // Table Name
        private static final int DATABASE_Version = 1;    // Database Version
        //private static final String UID="_id";     // Column I (Primary Key)
        //private static final String NAME = "Name";    //Column II
        //private static final String MyPASSWORD= "Password";    // Column III
        private static final String ENCKEY= "ENCKEY";
        private static final String ENCID = "ENCID";

        private static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+
                " ("+ENCKEY+" VARCHAR(300) PRIMARY KEY, "+ENCID+" VARCHAR(300));";
        private static final String DROP_TABLE ="DROP TABLE IF EXISTS "+TABLE_NAME;
        private Context context;


        public myDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_Version);
            this.context=context;
        }

        public void onCreate(SQLiteDatabase db) {

            try {
                db.execSQL(CREATE_TABLE);
            } catch (Exception e) {
                //Message.message(context,""+e);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            try {
                //Message.message(context,"OnUpgrade");
                db.execSQL(DROP_TABLE);
                onCreate(db);
            }catch (Exception e) {
                //Message.message(context,""+e);
            }
        }
    }
}
