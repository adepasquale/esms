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
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

import com.googlecode.esms.account.Account;
import com.googlecode.esms.account.AccountConnector;
import com.googlecode.esms.message.Base64;
import com.googlecode.esms.message.SMS;

public class Vodafone extends Account {
  private static final long serialVersionUID = 1L;

  static final String PROVIDER = "Vodafone";

  static final String CHECK_USER = 
    "https://www.vodafone.it/190/trilogy/jsp/utility/checkUser.jsp";
  static final String DO_LOGIN = 
    "https://www.vodafone.it/190/trilogy/jsp/login.do";
//  static final String DO_LOGOUT = 
//    "http://www.vodafone.it/190/trilogy/jsp/logout.do";
  static final String DO_SWAPSIM = 
    "https://www.vodafone.it/190/trilogy/jsp/swapSim.do";
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
  public List<Result> send(SMS sms) {
    int currReceiver = 0;
    int totReceivers = sms.getReceivers().size();
    List<Result> results = new LinkedList<Result>();
    for (int r = 0; r < totReceivers; ++r)
      results.add(Result.UNKNOWN_ERROR);
    
    try {
      
      Result login = login();
      if (login != Result.SUCCESSFUL) {
        results.set(0, login);
        return results;
      }
      
      if (!senderCurrent.equalsIgnoreCase(sender)) {
        if (!doSwapSIM()) {
          results.set(0, Result.SENDER_ERROR);
          return results;
        }
      }

      for (int r = 0; r < totReceivers; ++r) {
        currReceiver = r;
        
        if (sms.getCaptchaText() == null || sms.getCaptchaText() == "") {
          int precheck = doPrecheck();
          if (precheck != 0) {
            results.set(r, getResult(precheck));
            return results;
          }
          
          int prepare = doPrepare(sms, r);
          if (prepare != 0) {
            results.set(r, getResult(prepare));
            return results;
          }
          
          if (sms.getCaptchaArray() != null) {
            results.set(r, Result.CAPTCHA_NEEDED);
            return results;
          }
        }
        
        // filter wrong CAPTCHA characters
        String captchaText = sms.getCaptchaText();
        if (captchaText != null) {
          captchaText.replaceAll("[^1-9A-NP-Za-np-z]*", "");
          sms.setCaptchaText(captchaText);
        }
        
        int send = doSend(sms, r);
        if (send != 0) {
          results.set(r, getResult(send));
          return results;
        }
        if (sms.getCaptchaArray() != null) {
          results.set(r, Result.CAPTCHA_NEEDED);
          return results;
        }
        
        updateCount();
        count += calcFragments(sms.getMessage().length());
        results.set(r, Result.SUCCESSFUL);
      }
      
    } catch (JDOMException je) {
      je.printStackTrace();
      results.set(currReceiver, Result.PROVIDER_ERROR);
    } catch (Exception e) {
      e.printStackTrace();
      results.set(currReceiver, Result.NETWORK_ERROR);
    }
    
    return results;
  }

  private void checkUser() throws ClientProtocolException, IOException, 
      IllegalStateException, JDOMException {
    HttpGet request = new HttpGet(CHECK_USER);
    HttpResponse response = httpClient.execute(request, httpContext);
    
    PushbackReader reader = new PushbackReader(
        new InputStreamReader(response.getEntity().getContent()));
    
    // fix wrong XML header 
    int first = reader.read();
    while (first != 60)
      first = reader.read();
    reader.unread(first);
    
    Document document = new SAXBuilder().build(reader);
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
      senderList.add(senderCurrent); // TODO support other SIM
//      Element asChild = userChild.getChild("all-sims");
//      @SuppressWarnings("unchecked")
//      List<Element> mChildren = asChild.getChildren("msisdn");
//      for (Element mChild : mChildren) senderList.add(mChild.getValue());
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
    case 107: 
      return Result.LIMIT_ERROR;
    case 113:
      return Result.RECEIVER_ERROR;
    case 109:
    case 112: // "Numero destinatario con caratteri non validi"
      return Result.MESSAGE_ERROR;
    case 104: // "Servizio non disponibile"
      return Result.PROVIDER_ERROR;
    default:
      return Result.UNKNOWN_ERROR;
    }
  }
  
  private boolean doSwapSIM() throws ClientProtocolException, 
      IOException, IllegalStateException, JDOMException {
    HttpPost request = new HttpPost(DO_SWAPSIM);
    List<NameValuePair> requestData = new ArrayList<NameValuePair>();
    requestData.add(new BasicNameValuePair("ty_sim", sender));
    requestData.add(new BasicNameValuePair("swap_sim_link", sender));
    request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.UTF_8));
    HttpResponse response = httpClient.execute(request, httpContext);
    response.getEntity().consumeContent();
    
    checkUser();
    if (loggedIn && usernameCurrent.equalsIgnoreCase(username) && 
        senderCurrent.equalsIgnoreCase(sender)) return true;
    else return false;
  }

  private int doPrecheck() throws ClientProtocolException, 
      IOException, JDOMException {
    HttpGet request = new HttpGet(DO_PRECHECK);
    HttpResponse response = httpClient.execute(request, httpContext);

    PushbackReader reader = new PushbackReader(
        new InputStreamReader(response.getEntity().getContent()));
    
    // fix wrong XML header 
    int first = reader.read();
    while (first != 60)
      first = reader.read();
    reader.unread(first);
    
    Document document = new SAXBuilder().build(reader);
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
    }

    if (status != 1)
      return errorcode;
    return 0;
  }

  private String stripPrefix(String receiver) {
    final String PREFIX = "39";
    receiver = receiver.replaceAll("[^0-9\\+]*", "");
    int lNumber = receiver.length();

    String pPlus = "+" + PREFIX;
    int lPlus = pPlus.length();
    if (lNumber > lPlus && receiver.substring(0, lPlus).equals(pPlus)) {
      return receiver.substring(lPlus);
    }

    String pZero = "00" + PREFIX;
    int lZero = pZero.length();
    if (lNumber > lZero && receiver.substring(0, lZero).equals(pZero)) {
      return receiver.substring(lZero);
    }
    
    return receiver;
  }
  
  private int doPrepare(SMS sms, int id) throws IOException, 
      IllegalStateException, JDOMException {
    HttpPost request = new HttpPost(DO_PREPARE);
    List<NameValuePair> requestData = new ArrayList<NameValuePair>();
    requestData.add(new BasicNameValuePair("receiverNumber", 
        stripPrefix(sms.getReceivers().get(id).getNumber())));
    requestData.add(new BasicNameValuePair("message", sms.getMessage()));
    request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.UTF_8));
    HttpResponse response = httpClient.execute(request, httpContext);
    
    PushbackReader reader = new PushbackReader(
        new InputStreamReader(response.getEntity().getContent()));
    
    // fix wrong XML header 
    int first = reader.read();
    while (first != 60)
      first = reader.read();
    reader.unread(first);
    
    Document document = new SAXBuilder().build(reader);
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
    requestData.add(new BasicNameValuePair("verifyCode", sms.getCaptchaText()));
    requestData.add(new BasicNameValuePair("receiverNumber", 
        stripPrefix(sms.getReceivers().get(id).getNumber())));
    requestData.add(new BasicNameValuePair("message", sms.getMessage()));
    request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.UTF_8));
    HttpResponse response = httpClient.execute(request, httpContext);
    
    PushbackReader reader = new PushbackReader(
        new InputStreamReader(response.getEntity().getContent()));
    
    // fix wrong XML header 
    int first = reader.read();
    while (first != 60)
      first = reader.read();
    reader.unread(first);
    
    Document document = new SAXBuilder().build(reader);
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
    
    sms.setCaptchaArray(null);
    return 0;
  }
}
