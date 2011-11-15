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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import com.googlecode.esms.account.Account;
import com.googlecode.esms.account.AccountConnector;
import com.googlecode.esms.message.Receiver;
import com.googlecode.esms.message.SMS;

public class Tim extends Account {
  private static final long serialVersionUID = 1L;

  static final String PROVIDER = "TIM";
  
  static final String DO_LOGIN = 
      "https://www.tim.it/authfe/login.do";
  static final String DO_LOGOUT = 
      "https://www.119selfservice.tim.it/119/logout.do";
  static final String ADD_DISPATCH_NEW =
      "https://smsweb.tim.it/sms-web/adddispatch?start=new";
  static final String ADD_DISPATCH_FORM =
      "https://smsweb.tim.it/sms-web/adddispatch.adddispatchform";
  static final String VALIDATE_CAPTCHA_IMAGE = 
      "https://smsweb.tim.it/sms-web/validatecaptcha:image/false?t:ac=Dispatch";
  static final String VALIDATE_CAPTCHA_FORM = 
      "https://smsweb.tim.it/sms-web/validatecaptcha.validatecaptchaform";
  
  static final String RE_SENDER_LIST = 
      "<option\\s+([^>]*\\s+)?value=\"([0-9]{9,10})\"\\s*([^>]*)?>";
  static final Pattern PATTERN_LOGIN = Pattern.compile(
      "("+RE_SENDER_LIST+")|("+DO_LOGOUT+")");
  
  static final String RE_SESSION_EXPIRED =
      "<span\\s+([^>]*\\s+)?class=\"errore\"\\s*([^>]*)?>([^<]*)</span>";
  static final String RE_MESSAGE_COUNT = 
      "<img\\s+([^>]*\\s+)?alt=\"SMS inviati\"\\s*([^>]*)?>" +
      "<div\\s*([^>]*)?>\\s*([0-5])\\s*</div>";
  static final String RE_FORM_DATA =
      "<input\\s+([^>]*\\s+)?name=\"t:formdata\"\\s+([^>]*\\s+)?" +
      "value=\"([^\"]*)\"\\s*([^>]*)?>";
  static final Pattern PATTERN_ADD_DISPATCH_NEW = Pattern.compile(
      "("+RE_SESSION_EXPIRED+")|("+RE_MESSAGE_COUNT+")|("+RE_FORM_DATA+")");
  static final Pattern PATTERN_ADD_DISPATCH_FORM = Pattern.compile(
      "("+RE_FORM_DATA+")");
  
  boolean loggedIn;
  String senderCurrent;
  List<String> senderList;
  String formData, separateFreeNumbers;

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
    try {
      
      if (!loggedIn) {
        return Result.SUCCESSFUL;
      }
      
      if (doLogout()) {
        return Result.SUCCESSFUL;
      } else {
        return Result.LOGOUT_ERROR;
      }
      
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
    List<Result> results = new LinkedList<Result>();
    for (int r = 0; r < sms.getReceivers().size(); ++r)
      results.add(Result.UNKNOWN_ERROR);
    
    try {
      
      Result login = login();
      if (login != Result.SUCCESSFUL) {
        results.set(0, login);
        return results;
      }
      
      // TODO swap SIM here

      if (sms.getCaptchaText() == null || sms.getCaptchaText() == "") {
        
        Result addDispatchNew = addDispatchNew();
        if (addDispatchNew == Result.LOGIN_ERROR) {
          Result relogin = login();
          if (relogin != Result.SUCCESSFUL) {
            results.set(0, relogin);
            return results;
          }
          addDispatchNew = addDispatchNew();
        }
        
        if (addDispatchNew != Result.SUCCESSFUL) {
          results.set(0, addDispatchNew);
          return results;
        }
        
        results.set(0, addDispatchForm(sms));
        return results;
        
      } else {
      
//      int send = doSend(sms);
//      if (send != 0) {
//        results.set(r, getResult(send));
//        return results;
//      }
//      if (sms.getCaptchaArray() != null) {
//        results.set(r, Result.CAPTCHA_NEEDED);
//        return results;
//      }
//      
//      updateCount();
//      count += calcFragments(sms.getMessage().length());
//      results.set(r, Result.SUCCESSFUL);
        
      }
      
    } catch (Exception e) {
      e.printStackTrace();
      results.set(0, Result.NETWORK_ERROR);
    }
    
    return results;
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
  
  private List<String> findPattern(InputStream source, Pattern pattern) {
    List<String> results = new ArrayList<String>();
    Scanner scanner = new Scanner(source, "UTF-8");

    String match = "";
    while (match != null) {
      match = scanner.findWithinHorizon(pattern, 0);
      if (match != null) results.add(match);
    }
    
    return results;
  }

  private byte[] toByteArray(InputStream is) throws IOException {
    int read;
    byte[] data = new byte[16*1024]; // 16 kB
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    while ((read = is.read(data, 0, data.length)) != -1)
      buffer.write(data, 0, read);
    buffer.flush();
    return buffer.toByteArray();
  }
  
  private boolean doLogin() throws ClientProtocolException, IOException {
    HttpPost request = new HttpPost(DO_LOGIN);
    List<NameValuePair> requestData = new ArrayList<NameValuePair>();
    requestData.add(new BasicNameValuePair("login", username));
    requestData.add(new BasicNameValuePair("password", password));
    requestData.add(new BasicNameValuePair("portale", "timPortale"));
    requestData.add(new BasicNameValuePair("urlOk", 
        "https://www.119selfservice.tim.it/119/consumerdispatcher"));
    request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.UTF_8));
    HttpResponse response = httpClient.execute(request, httpContext);
    List<String> strings = findPattern(
        response.getEntity().getContent(), PATTERN_LOGIN);
    response.getEntity().consumeContent();

    loggedIn = strings.contains(DO_LOGOUT);
    if (loggedIn) {
      strings.remove(DO_LOGOUT);
      Pattern number = Pattern.compile("([0-9]{9,10})");
      for (String s : strings) {
        Matcher m = number.matcher(s);
        if (m.find()) {
//          senderList.add(m.group()); // TODO support other SIM
          if (s.contains("selected=\"selected\"")) {
            senderList.add(m.group());
            senderCurrent = m.group();
          }
        }
      }
    }
    
    return loggedIn;
  }

  private boolean doLogout() throws ClientProtocolException, IOException {
//    HttpGet request = new HttpGet(DO_LOGOUT);
//    HttpResponse response = httpClient.execute(request, httpContext);
//    response.getEntity().consumeContent();
    cookieStore.clear();
    return true;
  }
  
  private Result addDispatchNew() {
    try {
      
      HttpGet request = new HttpGet(ADD_DISPATCH_NEW);
      HttpResponse response = httpClient.execute(request, httpContext);
      List<String> strings = findPattern(
          response.getEntity().getContent(), PATTERN_ADD_DISPATCH_NEW);
      response.getEntity().consumeContent();
      
      Pattern session = Pattern.compile(RE_SESSION_EXPIRED);
      for (String s : strings) {
        Matcher m = session.matcher(s);
        if (m.find()) return Result.LOGIN_ERROR;
      }
      
      Pattern counter = Pattern.compile(RE_MESSAGE_COUNT);
      for (String s : strings) {
        Matcher m = counter.matcher(s);
        if (m.find()) 
          count = Integer.parseInt(m.group(4));
      }
      
      if (count == limit)
        return Result.LIMIT_ERROR;
      
      formData = null;
      separateFreeNumbers = null;
      Pattern data = Pattern.compile(RE_FORM_DATA);
      for (String s : strings) {
        Matcher m = data.matcher(s);
        if (m.find()) {
          if (s.contains("id=")) {
            if (s.contains("seperateFreeNumbers:hidden"))
              separateFreeNumbers = m.group(3);
          } else {
            formData = m.group(3);
          }
        }
      }

      if (formData == null || separateFreeNumbers == null)
        return Result.PROVIDER_ERROR;
      
      return Result.SUCCESSFUL;
      
    } catch (Exception e) {
      e.printStackTrace();
      return Result.NETWORK_ERROR;
    }
  }
  
  private Result addDispatchForm(SMS sms) {
    List<Receiver> receivers = sms.getReceivers();
    String freeNumbers = stripPrefix(receivers.get(0).getNumber());
    for (int r = 1; r < receivers.size(); ++r)
      freeNumbers += ","+stripPrefix(receivers.get(r).getNumber());
    
    try {
      
      HttpPost postRequest = new HttpPost(ADD_DISPATCH_FORM);
      List<NameValuePair> postRequestData = new ArrayList<NameValuePair>();
      postRequestData.add(new BasicNameValuePair("t:formdata", formData));
      postRequestData.add(new BasicNameValuePair("recipientType", "FREE_NUMBERS"));
      postRequestData.add(new BasicNameValuePair("t:formdata", separateFreeNumbers));
      postRequestData.add(new BasicNameValuePair("freeNumbers", freeNumbers));
      postRequestData.add(new BasicNameValuePair("t:formdata", ""));
      postRequestData.add(new BasicNameValuePair("contactListId", ""));
      postRequestData.add(new BasicNameValuePair("t:formdata", ""));
      postRequestData.add(new BasicNameValuePair("contactsIdString", ""));
      postRequestData.add(new BasicNameValuePair("deliverySmsClass", "STANDARD"));
      postRequestData.add(new BasicNameValuePair("textAreaStandard", sms.getMessage()));
      postRequestData.add(new BasicNameValuePair("textAreaFlash", ""));
      postRequestData.add(new BasicNameValuePair("modelsSelect", ""));
      postRequest.setEntity(new UrlEncodedFormEntity(postRequestData, HTTP.UTF_8));
      HttpResponse postResponse = httpClient.execute(postRequest, httpContext);
      List<String> strings = findPattern(
          postResponse.getEntity().getContent(), PATTERN_ADD_DISPATCH_FORM);
      postResponse.getEntity().consumeContent();
      
      formData = null;
      Pattern data = Pattern.compile(RE_FORM_DATA);
      for (String s : strings) {
        Matcher m = data.matcher(s);
        if (m.find()) formData = m.group(3);
      }

      if (formData == null)
        return Result.PROVIDER_ERROR;

      HttpGet getRequest = new HttpGet(VALIDATE_CAPTCHA_IMAGE);
      HttpResponse getResponse = httpClient.execute(getRequest, httpContext);
      if (getResponse.getStatusLine().getStatusCode() != 200)
        return Result.PROVIDER_ERROR;
      
      sms.setCaptchaArray(toByteArray(getResponse.getEntity().getContent()));
      getResponse.getEntity().consumeContent();
      return Result.CAPTCHA_NEEDED;
      
    } catch (Exception e) {
      e.printStackTrace();
      return Result.NETWORK_ERROR;
    }
  }
  
//  private Result validateCaptchaForm(SMS sms) {
//    
//  }
}
