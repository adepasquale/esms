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

package com.googlecode.esms.account;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.protocol.HttpContext;

import com.googlecode.esms.message.SMS;

/**
 * Abstract class for every account used to send.  
 * @author Andrea De Pasquale
 */
public abstract class Account implements Serializable {
  private static final long serialVersionUID = 1L;

  /**
   * Values returned by some of Account class methods.
   */
  public enum Result {
    SUCCESSFUL,
    CAPTCHA_NEEDED,
    CAPTCHA_ERROR,
    NETWORK_ERROR,
    LOGIN_ERROR,
    LOGOUT_ERROR,
    SENDER_ERROR,
    RECEIVER_ERROR,
    MESSAGE_ERROR,
    LIMIT_ERROR,
    UNSUPPORTED_ERROR,
    PROVIDER_ERROR,
    UNKNOWN_ERROR
  }

  transient protected HttpClient httpClient;
  transient protected HttpContext httpContext;
  transient protected CookieStore cookieStore;

  protected String label;
  protected String provider;

  protected String username;
  protected String password;

  protected String sender;

  protected int limit;
  protected int count;
  protected Date countDate;

  /**
   * Account constructor with specified connector.
   * @param connector some connector implementation
   */
  public Account(AccountConnector connector) {
    setAccountConnector(connector);
    count = 0;
    countDate = new Date();
  }

  /**
   * Change the current account connector.
   * @param connector the new connector
   */
  public void setAccountConnector(AccountConnector connector) {
    if (connector != null) {
      httpClient = connector.getHttpClient();
      httpContext = connector.getHttpContext();
      cookieStore = connector.getCookieStore();
      httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
      initAccountConnector();
    }
  }
  
  /**
   * Convenience method called after setAccountConnector()
   */
  protected void initAccountConnector() {
    // empty default implementation
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
  
  public int getLimit() {
    return limit;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count, Date countDate) {
    this.count = count;
    this.countDate = countDate;
    updateCount();
  }
  
  /**
   * Convenience method called after setCount()
   */
  protected void updateCount() {
    // empty default implementation
  }
  
  public Date getCountDate() {
    return countDate;
  }

  /**
   * Calculate how many characters are remaining.
   * @param length current message length
   * @return Number of chars remaining. 
   */
  public abstract int calcRemaining(int length);
  
  /**
   * Calculate how many fragments are needed.
   * @param length current message length
   * @return Number of fragments needed.
   */  
  public abstract int calcFragments(int length);
  
  /**
   * Get available senders for this account.
   * @return List of valid senders.
   */
  public abstract List<String> getSenderList();
  
  /**
   * Perform login for this account.
   * @return enum value, Result.SUCCESSFUL if ok.
   */
  public abstract Result login();
  
  /**
   * Perform logout for this account.
   * @return enum value, Result.SUCCESSFUL if ok.
   */
  public abstract Result logout();
  
  /**
   * Send an SMS using this account.
   * @param sms Message with parameters
   * @return enum value, Result.SUCCESSFUL if ok.
   */
  public abstract Result send(SMS sms);
}
