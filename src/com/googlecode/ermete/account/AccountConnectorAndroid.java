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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.KeyStore;
import java.util.Date;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.content.Context;

import com.googlecode.ermete.R;

public class AccountConnectorAndroid extends AccountConnector {
  private static final long serialVersionUID = 1L;

  Context context;

  public AccountConnectorAndroid(Context context) {
    this.context = context;
  }

  public HttpClient getHttpClient() {
    return new HttpClientAndroid(context);
  }

  public HttpContext getHttpContext() {
    return new BasicHttpContext();
  }

  public CookieStore getCookieStore() {
    return new CookieStoreAndroid();
  }

  // TODO check if keystore should be generated again
  class HttpClientAndroid extends DefaultHttpClient {

    Context context;

    public HttpClientAndroid(Context context) {
      this.context = context;
    }

    @Override
    protected ClientConnectionManager createClientConnectionManager() {
      SchemeRegistry registry = new SchemeRegistry();
      registry.register(
          new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
      registry.register(new Scheme("https", newSSLSocketFactory(), 443));
      return new SingleClientConnManager(getParams(), registry);
    }

    private SSLSocketFactory newSSLSocketFactory() {
      try {
        KeyStore trusted = KeyStore.getInstance("BKS");
        InputStream in = context.getResources().openRawResource(R.raw.keystore);

        try {
          trusted.load(in, "storepass".toCharArray());
        } finally {
          in.close();
        }

        SSLSocketFactory sf = new SSLSocketFactory(trusted);
        sf.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
        return sf;

      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  // TODO implement CookieStore without using BasicCookieStore
  class CookieStoreAndroid implements CookieStore, Serializable {
    static final long serialVersionUID = 1L;

    transient CookieStore cookieStore;

    public CookieStoreAndroid() {
      cookieStore = new BasicCookieStore();
    }

    @Override
    public void addCookie(Cookie cookie) {
      cookieStore.addCookie(cookie);
    }

    @Override
    public void clear() {
      cookieStore.clear();
    }

    @Override
    public boolean clearExpired(Date date) {
      return cookieStore.clearExpired(date);
    }

    @Override
    public List<Cookie> getCookies() {
      return cookieStore.getCookies();
    }

    private void readObject(ObjectInputStream input) throws IOException,
        ClassNotFoundException {
      cookieStore = new BasicCookieStore();

      input.defaultReadObject();
      int size = input.readInt();
      while (size-- > 0) {
        CookieAndroid wsCookie = (CookieAndroid) input.readObject();
        cookieStore.addCookie(wsCookie.getCookie());
      }
    }

    private void writeObject(ObjectOutputStream output) throws IOException {
      output.defaultWriteObject();
      List<Cookie> cookies = cookieStore.getCookies();
      output.writeInt(cookies.size());
      for (Cookie cookie : cookies) {
        output.writeObject(new CookieAndroid(cookie));
      }
    }

    // TODO implement Cookie interface entirely
    class CookieAndroid implements Serializable {
      static final long serialVersionUID = 1L;

      int version;
      String name;
      String value;
      String domain;
      String path;
      long expiry;

      public CookieAndroid(Cookie cookie) {
        version = cookie.getVersion();
        name = cookie.getName();
        value = cookie.getValue();
        domain = cookie.getDomain();
        path = cookie.getPath();

        expiry = 0;
        Date expiryDate = cookie.getExpiryDate();
        if (expiryDate != null)
          expiry = expiryDate.getTime();
      }

      public Cookie getCookie() {
        BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setVersion(version);
        cookie.setDomain(domain);
        cookie.setPath(path);
        if (expiry != 0)
          cookie.setExpiryDate(new Date(expiry));
        return cookie;
      }
    }
  }

}
