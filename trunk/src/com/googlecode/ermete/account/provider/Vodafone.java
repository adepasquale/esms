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

package com.googlecode.ermete.account.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.googlecode.ermete.account.Account;
import com.googlecode.ermete.account.AccountConnector;
import com.googlecode.ermete.sms.SMS;

// TODO web, widget and business all in one account
public class Vodafone extends Account {
  private static final long serialVersionUID = 1L;

  static final String PROVIDER = "Vodafone";

  public Vodafone(AccountConnector connector) {
    super(connector);

    label = PROVIDER;
    provider = PROVIDER;
    limit = 10;
    
    loggedIn = false;
    senderList = new LinkedList<String>();
  }

  @Override
  protected void initAccountConnector() {
    httpClient.getParams().setParameter(
        "http.protocol.allow-circular-redirects", true);
    httpClient.getParams().setParameter("http.useragent", "Vodafone_DW");
  }
  
  @Override
  public int calcRemaining(int length) {
    if (length == 0) {
      return 0;
    } else if (length <= 160) {
      return 160 - length;
    } else if (length <= 307) {
      return 307 - length;
    } else if (length <= 360) {
      return 360 - length;
    } else {
      return 360 - length;
    }
  }

  @Override
  public int calcFragments(int length) {
    if (length == 0) {
      return 0;
    } else if (length <= 160) {
      return 1;
    } else if (length <= 307) {
      return 2;
    } else if (length <= 360) {
      return 3;
    } else {
      return 0;
    }
  }

  @Override
  public Result login() {
    try {
      
      if (loggedIn) {
        if (usernameCurrent.equalsIgnoreCase(username)) {
          return Result.SUCCESSFUL;
        } else if (!doLogout()) {
          return Result.LOGOUT_ERROR;
        }
      }
      
      if (doLogin()) {
        if (isCorporate || isBlocked) 
          return Result.UNSUPPORTED_ERROR;
        else return Result.SUCCESSFUL;
      } else {
        return Result.LOGIN_ERROR;
      }
      
    } catch (JDOMException je) {
      je.printStackTrace();
      return Result.PROVIDER_ERROR;
    } catch (Exception e) {
      e.printStackTrace();
      return Result.NETWORK_ERROR;
    }
  }
  
  @Override
  public Result logout() {
    try {
      
      if (!loggedIn) {
        return Result.SUCCESSFUL;
      }
      
      if (doLogout()) {
        return Result.SUCCESSFUL;
      } else {
        return Result.LOGOUT_ERROR;
      }
      
    } catch (JDOMException je) {
      je.printStackTrace();
      return Result.PROVIDER_ERROR;
    } catch (Exception e) {
      e.printStackTrace();
      return Result.NETWORK_ERROR;
    }
  }  

  @Override
  public List<String> getSenderList() {
    if (loggedIn) return senderList;
    else return null;
  }

  @Override
  public Result send(SMS sms) {
    // TODO Auto-generated method stub
    return Result.SUCCESSFUL;
  }

  private static final String CHECK_USER = 
    "https://www.vodafone.it/190/trilogy/jsp/utility/checkUser.jsp";
  private static final String DO_LOGIN = 
    "https://www.vodafone.it/190/trilogy/jsp/login.do";
//  private static final String DO_LOGOUT = 
//    "http://www.vodafone.it/190/trilogy/jsp/logout.do";

  boolean loggedIn;
  boolean isConsumer;
  boolean isCorporate;
  boolean isBlocked;
  String senderCurrent;
  List<String> senderList;
  String usernameCurrent;
  
  private void checkUser() throws ClientProtocolException, IOException, 
      IllegalStateException, JDOMException {
    HttpGet request = new HttpGet(CHECK_USER);
    HttpResponse response = httpClient.execute(request, httpContext);
    Document document = 
        new SAXBuilder().build(response.getEntity().getContent());
    response.getEntity().consumeContent();
    Element root = document.getRootElement();
    Element liChild = root.getChild("logged-in");
    loggedIn = liChild.getValue().equals("true");
    
    senderCurrent = null;
    senderList.clear();
    
    if (loggedIn) {
      Element userChild = root.getChild("user");
      isConsumer = userChild.getChild("is-consumer").getValue().equals("true");
      isCorporate = userChild.getChild("is-corporate").getValue().equals("true");
      isBlocked = userChild.getChild("is-blocked").getValue().equals("true");
      senderCurrent = userChild.getChild("current-msisdn").getValue();
      Element asChild = userChild.getChild("all-sims");
      @SuppressWarnings("unchecked")
      List<Element> mChildren = asChild.getChildren("msisdn");
      for (Element mChild : mChildren) senderList.add(mChild.getValue());
      usernameCurrent = root.getChild("security").getChild("username").getValue();
    }
  }
  
  private boolean doLogin() throws ClientProtocolException, IOException,
      IllegalStateException, JDOMException {
    HttpPost request = new HttpPost(DO_LOGIN);
    List<NameValuePair> requestData = new ArrayList<NameValuePair>();
    requestData.add(new BasicNameValuePair("username", username));
    requestData.add(new BasicNameValuePair("password", password));
    request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.UTF_8));
    HttpResponse response = httpClient.execute(request, httpContext);
    response.getEntity().consumeContent();
    
    checkUser();
    if (loggedIn && usernameCurrent.equalsIgnoreCase(username)) return true;
    else return false;
  }
  
  private boolean doLogout() throws ClientProtocolException, 
      IOException, IllegalStateException, JDOMException {
//    HttpGet request = new HttpGet(DO_LOGOUT);
//    HttpResponse response = httpClient.execute(request, httpContext);
//    response.getEntity().consumeContent();

    // this is enough
    cookieStore.clear();
    
    checkUser();
    if (!loggedIn) return true;
    else return false;
  }  

}
