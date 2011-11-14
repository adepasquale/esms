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

package com.googlecode.esms.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import com.googlecode.esms.account.Account;
import com.googlecode.esms.account.AccountConnector;
import com.googlecode.esms.message.SMS;

public class Tim extends Account {
  private static final long serialVersionUID = 1L;

  static final String PROVIDER = "TIM";
  
  static final String DO_LOGIN = 
      "https://www.tim.it/authfe/login.do";
  static final String DO_LOGIN_URL_OK = 
      "https://www.119selfservice.tim.it/119/consumerdispatcher";
  static final String DO_LOGOUT = 
      "https://www.119selfservice.tim.it/119/logout.do";
  
  boolean loggedIn;
  String senderCurrent;
  List<String> senderList;

  public Tim(AccountConnector connector) {
    super(connector);

    label = PROVIDER;
    provider = PROVIDER;
    limit = 5;
    
    loggedIn = false;
    senderList = new LinkedList<String>();
  }

  @Override
  public int calcRemaining(int length) {
    if (length == 0) {
      return 0;
    } else {
      return 640-length;
    }
  }

  @Override
  public int calcFragments(int length) {
    if (length > 0 && length <= 640) {
      return 1;
    } else {
      return 0;
    }
  }
  
  @Override
  protected void updateCount() {
    Calendar prev = Calendar.getInstance();
    prev.setTime(countDate);
    Calendar curr = Calendar.getInstance();
    curr.setTime(new Date());
    if (prev.get(Calendar.YEAR) < curr.get(Calendar.YEAR) ||
        prev.get(Calendar.MONTH) < curr.get(Calendar.MONTH) || 
        prev.get(Calendar.DATE) < curr.get(Calendar.DATE)) {
      count = 0;
      countDate = new Date();
    }
  }

  @Override
  public Result login() {
    try {
      
      if (loggedIn) {
        if (senderList.contains(username))
          return Result.SUCCESSFUL;
        else if (!doLogout())
          return Result.LOGOUT_ERROR;
      }
      
      if (doLogin()) {
        return Result.SUCCESSFUL;
      } else {
        return Result.LOGIN_ERROR;
      }
      
    } catch (Exception e) {
      e.printStackTrace();
      return Result.NETWORK_ERROR;
    }
  }

  @Override
  public Result logout() {
    return Result.UNSUPPORTED_ERROR;
  }
  
  @Override
  public List<String> getSenderList() {
    if (loggedIn) return senderList;
    else return null;
  }

  @Override
  public List<Result> send(SMS sms) {
    List<Result> results = new LinkedList<Result>();
    for (int r = 0; r < sms.getReceivers().size(); ++r)
      results.add(Result.UNSUPPORTED_ERROR);
    return results;
  }
  
  private List<String> findRegex(InputStream source, String regex) {
    List<String> results = new ArrayList<String>();
    Scanner scanner = new Scanner(source, "UTF-8");
    Pattern pattern = Pattern.compile(regex);
    return null;
  }

  private boolean doLogin() throws ClientProtocolException, IOException {
    HttpPost request = new HttpPost(DO_LOGIN);
    List<NameValuePair> requestData = new ArrayList<NameValuePair>();
    requestData.add(new BasicNameValuePair("login", username));
    requestData.add(new BasicNameValuePair("password", password));
    requestData.add(new BasicNameValuePair("portale", "timPortale"));
    requestData.add(new BasicNameValuePair("urlOk", DO_LOGIN_URL_OK));
    request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.UTF_8));
    HttpResponse response = httpClient.execute(request, httpContext);
    Scanner scanner = new Scanner(response.getEntity().getContent(), "UTF-8");
//    Pattern logoutPattern = Pattern.compile(DO_LOGOUT);
//    System.out.println(scanner.findWithinHorizon(logoutPattern, 0));
    Pattern numberPattern = Pattern.compile(
        "<option\\s+([^>]*\\s+)?value=\"([0-9]{9,10})\"\\s*([^>]*)?>");
    scanner.findWithinHorizon(numberPattern, 0);
    System.out.println(scanner.match().group(2));
    
    // TODO loggedIn
    // TODO senderList
    
    response.getEntity().consumeContent();
    return false;
  }

  private boolean doLogout() throws ClientProtocolException, IOException {
//    HttpGet request = new HttpGet(DO_LOGOUT);
//    HttpResponse response = httpClient.execute(request, httpContext);
//    response.getEntity().consumeContent();
    cookieStore.clear();
    return true;
  }
}
