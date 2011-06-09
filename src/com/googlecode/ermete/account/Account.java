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

import java.io.Serializable;

import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.protocol.HttpContext;

import com.googlecode.ermete.sms.SMS;

public abstract class Account implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum Result {
    SUCCESSFUL, 
    NETWORK_ERROR, 
    LOGIN_ERROR, 
    RECEIVER_ERROR, 
    MESSAGE_ERROR, 
    LIMIT_ERROR, 
    UNKNOWN_ERROR
  }

  transient protected HttpClient httpClient;
  transient protected HttpContext httpContext;
  transient protected CookieStore cookieStore;

  protected String label;
  protected String provider;

  // FIXME this is platform-dependent
  protected int logoID;

  protected String username;
  protected String password;

  protected String sender;

  protected int count;
  protected int limit;

  public Account(AccountConnector connector) {
    setAccountConnector(connector);
  }

  public void setAccountConnector(AccountConnector connector) {
    httpClient = connector.getHttpClient();
    httpContext = connector.getHttpContext();
    cookieStore = connector.getCookieStore();
    httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
  }
  
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
    if (label.length() > 12)
      this.label = label.substring(0, 12);
  }

  public String getProvider() {
    return provider;
  }

  public int getLogoID() {
    return logoID;
  }

  public void setLogoID(int logoID) {
    this.logoID = logoID;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public int getCount() {
    return count;
  }

  public int getLimit() {
    return limit;
  }

  public abstract int calcRemaining(int length);

  public abstract int calcFragments(int length);

  public abstract Result login();

  public abstract String[] info();

  public abstract Result send(SMS sms);
}
