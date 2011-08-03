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

package com.googlecode.awsms.account;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.googlecode.esms.account.AccountConnector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

public class AccountConnectorAndroid extends AccountConnector {
  private static final long serialVersionUID = 1L;

  Context context;

  public AccountConnectorAndroid(Context context) {
    this.context = context;
  }

  public HttpClient getHttpClient() {
    return new DefaultHttpClient();
  }

  public HttpContext getHttpContext() {
    return new BasicHttpContext();
  }

  public CookieStore getCookieStore() {
    return new CookieStoreAndroid(context);
  }

  class CookieStoreAndroid implements CookieStore {

    static final String DB_NAME = "cookies.db";
    static final int DB_VERSION = 1;
    static final String TABLE_NAME = "cookies";

    public static final String _ID = "_id";
    public static final String VERSION = "version";
    public static final String NAME = "name";
    public static final String VALUE = "value";
    public static final String DOMAIN = "domain";
    public static final String PATH = "path";
    public static final String EXPIRY = "expiry";
    
    private class DBOpenHelper extends SQLiteOpenHelper {
      DBOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
      }

      @Override
      public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + _ID
            + " INTEGER PRIMARY KEY," + VERSION + " INTEGER," + NAME + " TEXT,"
            + VALUE + " TEXT," + DOMAIN + " TEXT," + PATH + " TEXT, "
            + EXPIRY + " BIGINT );");
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
      }
    }
    
    DBOpenHelper dbOpenHelper;

    public CookieStoreAndroid(Context context) {
      dbOpenHelper = new DBOpenHelper(context);
    }

    @Override
    public void addCookie(Cookie cookie) {
      SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
      String whereClause = 
          NAME + "=? AND " + 
          DOMAIN + "=? AND " +
          PATH + "=?"; 
      String[] whereArgs = { 
          cookie.getName(),
          cookie.getDomain(), 
          cookie.getPath()};
      db.delete(TABLE_NAME, whereClause, whereArgs);
      
      ContentValues values = new ContentValues();
      values.put(VERSION, cookie.getVersion());
      values.put(NAME, cookie.getName());
      values.put(VALUE, cookie.getValue());
      values.put(DOMAIN, cookie.getDomain());
      values.put(PATH, cookie.getPath());
      long expiry = 0;
      Date expiryDate = cookie.getExpiryDate();
      if (expiryDate != null) expiry = expiryDate.getTime();
      values.put(EXPIRY, expiry);
      db.insert(TABLE_NAME, null, values);
      
      db.close();
    }

    @Override
    public void clear() {
      SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
      db.delete(TABLE_NAME, null, null);
      db.close();
    }

    @Override
    public boolean clearExpired(Date date) {
      SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
      String whereClause = EXPIRY + " < ?";
      String[] whereArgs = { String.valueOf(date.getTime()) };
      int deleted = db.delete(TABLE_NAME, whereClause, whereArgs);
      db.close();
      
      return (deleted > 0);
    }

    @Override
    public List<Cookie> getCookies() {
      List<Cookie> cookies = new ArrayList<Cookie>();
      SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
      queryBuilder.setTables(TABLE_NAME);
      SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
      String projection[] = { VERSION, NAME, VALUE, DOMAIN, PATH, EXPIRY }; 
      Cursor c = queryBuilder.query(db, projection, null, null, null, null, null);
      c.moveToFirst();
      
      while (!c.isAfterLast()) {
        BasicClientCookie cookie = new BasicClientCookie(
            c.getString(c.getColumnIndex(NAME)),
            c.getString(c.getColumnIndex(VALUE)));
        cookie.setVersion(c.getInt(c.getColumnIndex(VERSION)));
        cookie.setDomain(c.getString(c.getColumnIndex(DOMAIN)));
        cookie.setPath(c.getString(c.getColumnIndex(PATH)));
        long expiry = c.getLong(c.getColumnIndex(EXPIRY));
        if (expiry != 0) cookie.setExpiryDate(new Date(expiry));
        cookies.add(cookie);
        c.moveToNext();
      }

      c.close();
      db.close();
      return cookies;
    }
  }

}
