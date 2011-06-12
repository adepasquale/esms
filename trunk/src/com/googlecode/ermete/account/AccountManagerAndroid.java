/*
 *  This file is part of Ermete SMS.
 *  
 *  Ermete SMS is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  Ermete SMS is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with Ermete SMS.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */

package com.googlecode.ermete.account;

import java.lang.reflect.Constructor;
import java.util.LinkedList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

import com.googlecode.ermete.account.provider.Tim;
import com.googlecode.ermete.account.provider.Vodafone;

public class AccountManagerAndroid extends AccountManager {
  private static final long serialVersionUID = 1L;

  static final String DB_NAME = "accounts.db";
  static final int DB_VERSION = 1;
  static final String TABLE_NAME = "accounts";

  public static final String _ID = "_id";
  public static final String CLASS = "class";
  public static final String LABEL = "label";
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";
  public static final String SENDER = "sender";
  public static final String COUNT = "count";

  private static class DBOpenHelper extends SQLiteOpenHelper {
    DBOpenHelper(Context context) {
      super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + _ID
          + " INTEGER PRIMARY KEY," + CLASS + " TEXT," + LABEL + " TEXT,"
          + USERNAME + " TEXT," + PASSWORD + " TEXT," + SENDER + " TEXT,"
          + COUNT + " INTEGER );");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      onCreate(db);
    }
  }
  
  DBOpenHelper dbOpenHelper;
  AccountConnectorAndroid connector;
  
  public AccountManagerAndroid(Context context) {
    dbOpenHelper = new DBOpenHelper(context);
    connector = new AccountConnectorAndroid(context);

    providers = new LinkedList<Account>();
    providers.add(new Vodafone(connector));
    providers.add(new Tim(connector));

    accounts = new LinkedList<Account>();
    
    SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
    queryBuilder.setTables(TABLE_NAME);
    SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
    Cursor c = queryBuilder.query(db, null, null, null, null, null, null);
    c.moveToFirst();
    
    while (!c.isAfterLast()) {
      try {
        
        @SuppressWarnings("unchecked")
        Class<Account> accountClass = 
          (Class<Account>) Class.forName(c.getString(c.getColumnIndex(CLASS)));
        Constructor<Account> accountConstructor = 
          accountClass.getConstructor(AccountConnector.class);
        Account account = (Account) accountConstructor.newInstance(connector);
        account.setLabel(c.getString(c.getColumnIndex(LABEL)));
        account.setUsername(c.getString(c.getColumnIndex(USERNAME)));
        account.setPassword(c.getString(c.getColumnIndex(PASSWORD)));
        account.setSender(c.getString(c.getColumnIndex(SENDER)));
        account.setCount(c.getInt(c.getColumnIndex(COUNT)));
        accounts.add(account);
        
      } catch (Exception e) {
        e.printStackTrace();
        continue;
      }

      c.moveToNext();
    }

    c.close();
    db.close();
  }

  @Override
  public void insert(Account newAccount) {
    accounts.add(newAccount);
    
    ContentValues values = new ContentValues();
    values.put(CLASS, newAccount.getClass().getName());
    values.put(LABEL, newAccount.getLabel());
    values.put(USERNAME, newAccount.getUsername());
    values.put(PASSWORD, newAccount.getPassword());
    values.put(SENDER, newAccount.getSender());
    values.put(COUNT, newAccount.getCount());

    SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
    db.insert(TABLE_NAME, null, values);
    db.close();
  }

  @Override
  public void delete(Account oldAccount) {
    accounts.remove(oldAccount);
    
    SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
    String whereClause = String.format(
        "%s=\'%s\' AND %s=\'%s\' AND %s=\'%s\'" + 
        " AND %s=\'%s\' AND %s=\'%s\' AND %s=%d",
        CLASS, oldAccount.getClass().getName(),
        LABEL, oldAccount.getLabel(),
        USERNAME, oldAccount.getUsername(),
        PASSWORD, oldAccount.getPassword(),
        SENDER, oldAccount.getSender(),
        COUNT, oldAccount.getCount());
    db.delete(TABLE_NAME, whereClause, null);
    db.close();
  }
}
