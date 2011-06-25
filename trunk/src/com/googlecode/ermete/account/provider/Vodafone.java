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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
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
import com.googlecode.ermete.sms.captcha.Base64;

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
        if (usernameCurrent.equalsIgnoreCase(username))
          return Result.SUCCESSFUL;
        else if (!doLogout())
          return Result.LOGOUT_ERROR;
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
    try {
      for (int i = 0; i < sms.getReceiverNumber().length; ++i) {
        
        int precheck = doPrecheck();
        if (precheck != 0) 
          return getResult(precheck);
        
        int prepare = doPrepare(sms, i);
        if (prepare != 0)
          return getResult(prepare);
        
        // TODO check CAPTCHA
//        return Result.CAPTCHA_NEEDED;
        
        int send = doSend(sms, i);
        if (send != 0)
          return getResult(send);
        
        count -= calcFragments(sms.getMessage().length());
        
      }
    } catch (Exception e) {
      e.printStackTrace();
      return Result.NETWORK_ERROR;
    }
    
    return Result.SUCCESSFUL;
  }

  static final String CHECK_USER = 
    "https://www.vodafone.it/190/trilogy/jsp/utility/checkUser.jsp";
  static final String DO_LOGIN = 
    "https://www.vodafone.it/190/trilogy/jsp/login.do";
//  static final String DO_LOGOUT = 
//    "http://www.vodafone.it/190/trilogy/jsp/logout.do";
  static final String DO_PRECHECK = 
    "https://www.vodafone.it/190/fsms/precheck.do?channel=VODAFONE_DW";
  static final String DO_PREPARE = 
    "https://www.vodafone.it/190/fsms/prepare.do?channel=VODAFONE_DW";
  static final String DO_SEND = 
    "https://www.vodafone.it/190/fsms/send.do?channel=VODAFONE_DW";

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

  private Result getResult(int code) {
    switch (code) {
    case 107: return Result.LIMIT_ERROR;
    case 113: return Result.RECEIVER_ERROR;
    case 104: // ?
    case 109: // empty message
    default:  return Result.UNKNOWN_ERROR;
    }
  }

  private int doPrecheck() throws ClientProtocolException, 
      IOException, JDOMException {
    Document document = null;

    // to solve XML header problem
    boolean skip = true;
    boolean done = false;

    do {
      try {
        HttpGet request = new HttpGet(DO_PRECHECK);
        HttpResponse response = httpClient.execute(request, httpContext);

        Reader reader = new BufferedReader(
            new InputStreamReader(response.getEntity().getContent()));
        if (skip) reader.skip(13);

        document = new SAXBuilder().build(reader);
        response.getEntity().consumeContent();
        done = true;

      } catch (JDOMException jdom) {
        if (skip) skip = false;
        else throw jdom;
      }
    } while (!done);

    Element root = document.getRootElement();
    @SuppressWarnings("unchecked")
    List<Element> children = root.getChildren("e");
    int status = 0, errorcode = 0;
    for (Element child : children) {
      if (child.getAttributeValue("n").equals("STATUS"))
        status = Integer.parseInt(child.getAttributeValue("v"));
      if (child.getAttributeValue("n").equals("ERRORCODE"))
        errorcode = Integer.parseInt(child.getAttributeValue("v"));
    }

    if (status != 1)
      return errorcode;
    return 0;
  }

  private int doPrepare(SMS sms, int id) throws IOException, 
      IllegalStateException, JDOMException {
    HttpPost request = new HttpPost(DO_PREPARE);
    List<NameValuePair> requestData = new ArrayList<NameValuePair>();
    requestData.add(new BasicNameValuePair("receiverNumber", sms.getReceiverNumber()[id]));
    requestData.add(new BasicNameValuePair("message", sms.getMessage()));
    request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.UTF_8));
    HttpResponse response = httpClient.execute(request, httpContext);
    Document document = new SAXBuilder().build(response.getEntity().getContent());
    response.getEntity().consumeContent();

    Element root = document.getRootElement();
    @SuppressWarnings("unchecked")
    List<Element> children = root.getChildren("e");
    int status = 0, errorcode = 0;
    for (Element child : children) {
      if (child.getAttributeValue("n").equals("STATUS"))
        status = Integer.parseInt(child.getAttributeValue("v"));
      if (child.getAttributeValue("n").equals("ERRORCODE"))
        errorcode = Integer.parseInt(child.getAttributeValue("v"));
      if (child.getAttributeValue("n").equals("CODEIMG")) {
        sms.setCaptchaArray(Base64.decode(child.getValue()));
        return 0;
      }
    }

    if (status != 1)
      return errorcode;
    return 0;
  }

  private int doSend(SMS sms, int id) throws IOException, 
      IllegalStateException, JDOMException {
    HttpPost request = new HttpPost(DO_SEND);
    List<NameValuePair> requestData = new ArrayList<NameValuePair>();
    requestData.add(new BasicNameValuePair("verifyCode", sms.getCaptcha()));
    requestData.add(new BasicNameValuePair("receiverNumber", sms.getReceiverNumber()[id]));
    requestData.add(new BasicNameValuePair("message", sms.getMessage()));
    request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.UTF_8));
    HttpResponse response = httpClient.execute(request, httpContext);
    Document document = new SAXBuilder().build(response.getEntity().getContent());
    response.getEntity().consumeContent();

    Element root = document.getRootElement();
    @SuppressWarnings("unchecked")
    List<Element> children = root.getChildren("e");
    int status = 0, errorcode = 0;
//    String returnmsg = null;
    for (Element child : children) {
      if (child.getAttributeValue("n").equals("STATUS"))
        status = Integer.parseInt(child.getAttributeValue("v"));
      if (child.getAttributeValue("n").equals("ERRORCODE"))
        errorcode = Integer.parseInt(child.getAttributeValue("v"));
//      if (child.getAttributeValue("n").equals("RETURNMSG"))
//        returnmsg = child.getValue();
      if (child.getAttributeValue("n").equals("CODEIMG")) {
        sms.setCaptchaArray(Base64.decode(child.getValue()));
        return 0;
      }
    }

    if (status != 1)
      return errorcode;
    return 0;
  }
}
