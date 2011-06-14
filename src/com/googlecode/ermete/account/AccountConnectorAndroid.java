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
    
    List<Cookie> cookies;
    DBOpenHelper dbOpenHelper;

// TODO For best performance, the caller should follow these guidelines: 
// * Provide an explicit projection, to prevent reading data from storage that 
// aren't going to be used.
// * Use question mark parameter markers such as 'phone=?' instead of explicit 
// values in the selection parameter, so that queries that differ only by 
// those values will be recognized as the same for caching purposes.
    public CookieStoreAndroid(Context context) {
      cookies = new ArrayList<Cookie>();
      dbOpenHelper = new DBOpenHelper(context);

      SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
      queryBuilder.setTables(TABLE_NAME);
      SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
      Cursor c = queryBuilder.query(db, null, null, null, null, null, null);
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
    }

    @Override
    public void addCookie(Cookie newCookie) {
      for (Cookie oldCookie : cookies) {
        if (oldCookie.getName() == newCookie.getName() && 
            oldCookie.getDomain() == newCookie.getDomain() &&
            oldCookie.getPath() == newCookie.getPath()) {
          cookies.remove(oldCookie);
        }
      }
      
      cookies.add(newCookie);
      
      ContentValues values = new ContentValues();
      values.put(VERSION, newCookie.getVersion());
      values.put(NAME, newCookie.getName());
      values.put(VALUE, newCookie.getValue());
      values.put(DOMAIN, newCookie.getDomain());
      values.put(PATH, newCookie.getPath());
      long expiry = 0;
      Date expiryDate = newCookie.getExpiryDate();
      if (expiryDate != null) expiry = expiryDate.getTime();
      values.put(EXPIRY, expiry);

      SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
      String whereClause = String.format(
          "%s=\'%s\' AND %s=\'%s\' AND %s=\'%s\'",
          NAME, newCookie.getName(),
          DOMAIN, newCookie.getDomain(),
          PATH, newCookie.getPath());
      db.delete(TABLE_NAME, whereClause, null);
      db.insert(TABLE_NAME, null, values);
      db.close();
    }

    @Override
    public void clear() {
      cookies.clear();
      
      SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
      db.delete(TABLE_NAME, null, null);
      db.close();
    }

    @Override
    public boolean clearExpired(Date date) {
      boolean purged = false;
      for (Cookie cookie : cookies)
        if (cookie.isExpired(date)) {
          cookies.remove(cookie);
          purged = true;
        }
      
      SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
      String whereClause = String.format("%s < %d", EXPIRY, date.getTime());
      int deleted = db.delete(TABLE_NAME, whereClause, null);
      db.close();
      
      return purged || (deleted > 0);
    }

    @Override
    public List<Cookie> getCookies() {
      return cookies;
    }
  }

}
