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

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

import com.googlecode.awsms.R;
import com.googlecode.esms.account.AccountConnector;

public class AccountConnectorAndroid extends AccountConnector {
  private static final long serialVersionUID = 1L;

  Context context;

  public AccountConnectorAndroid(Context context) {
    this.context = context;
  }

  public HttpClient getHttpClient() {
    return new CustomKeyStoreHttpClient(context);
  }

  public HttpContext getHttpContext() {
    return new BasicHttpContext();
  }

  public CookieStore getCookieStore() {
    return new CookieStoreAndroid(context);
  }

  class CustomKeyStoreHttpClient extends DefaultHttpClient {
    final Context context;

    public CustomKeyStoreHttpClient(Context context) {
      this.context = context;
    }

    @Override
    protected ClientConnectionManager createClientConnectionManager() {
      SchemeRegistry registry = new SchemeRegistry();
      registry.register(
          new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
      registry.register(
          new Scheme("https", createCustomKeyStoreSSLSocketFactory(), 443));
//      return new SingleClientConnManager(getParams(), registry);
      return new ThreadSafeClientConnManager(getParams(), registry);
    }

    private SSLSocketFactory createCustomKeyStoreSSLSocketFactory() {
      try {
        KeyStore keystore = KeyStore.getInstance("BKS");
        InputStream in = context.getResources().openRawResource(R.raw.keystore);

        try {
          keystore.load(in, "storepass".toCharArray());
        } finally {
          in.close();
        }

        return new CustomKeyStoreSSLSocketFactory(keystore);

      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }
  
  public class CustomKeyStoreSSLSocketFactory extends SSLSocketFactory {
    protected SSLContext sslContext = SSLContext.getInstance("TLS");

    public CustomKeyStoreSSLSocketFactory(KeyStore keyStore)
        throws NoSuchAlgorithmException, KeyManagementException,
        KeyStoreException, UnrecoverableKeyException {
      super(null, null, null, null, null, null);
      sslContext.init(null, new TrustManager[] { 
          new AdditionalKeyStoresTrustManager(keyStore) }, null);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port,
        boolean autoClose) throws IOException {
      return sslContext.getSocketFactory().createSocket(
          socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket() throws IOException {
      return sslContext.getSocketFactory().createSocket();
    }
  }
  
  public static class AdditionalKeyStoresTrustManager 
      implements X509TrustManager {

    protected ArrayList<X509TrustManager> x509TrustManagers = 
        new ArrayList<X509TrustManager>();

    protected AdditionalKeyStoresTrustManager(KeyStore... additionalkeyStores) {
      final ArrayList<TrustManagerFactory> factories = 
          new ArrayList<TrustManagerFactory>();

      try {
        final TrustManagerFactory original = 
            TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        original.init((KeyStore) null);
        factories.add(original);

        for (KeyStore keyStore : additionalkeyStores) {
          final TrustManagerFactory additionalCerts = 
              TrustManagerFactory.getInstance(
                  TrustManagerFactory.getDefaultAlgorithm());
          additionalCerts.init(keyStore);
          factories.add(additionalCerts);
        }

      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      for (TrustManagerFactory tmf : factories)
        for (TrustManager tm : tmf.getTrustManagers())
          if (tm instanceof X509TrustManager)
            x509TrustManagers.add((X509TrustManager) tm);

      if (x509TrustManagers.size() == 0)
        throw new RuntimeException("Couldn't find any X509TrustManagers");
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
      final X509TrustManager defaultX509TrustManager = x509TrustManagers.get(0);
      defaultX509TrustManager.checkClientTrusted(chain, authType);
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
      for (X509TrustManager tm : x509TrustManagers) {
        try {
          tm.checkServerTrusted(chain, authType);
          return;
        } catch (CertificateException e) {
          // ignore until the end
        }
      }
      
      throw new CertificateException();
    }

    public X509Certificate[] getAcceptedIssuers() {
      final ArrayList<X509Certificate> list = new ArrayList<X509Certificate>();
      for (X509TrustManager tm : x509TrustManagers)
        list.addAll(Arrays.asList(tm.getAcceptedIssuers()));
      return list.toArray(new X509Certificate[list.size()]);
    }
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
      Cursor c = queryBuilder.query(
          db, projection, null, null, null, null, null);
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
