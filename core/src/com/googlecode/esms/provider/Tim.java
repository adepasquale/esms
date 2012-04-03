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

import javax.net.ssl.SSLException;

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
  
  static final String RE_LIMIT_ERROR =
      "<div\\s+([^>]*\\s+)?class=\"errore\"\\s*([^>]*)?>([^<]*)<br/>";
  static final String RE_GENERIC_ERROR =
      "<span\\s+([^>]*\\s+)?class=\"errore\"\\s*([^>]*)?>([^<]*)</span>";
  static final String RE_MESSAGE_COUNT = 
      "<img\\s+([^>]*\\s+)?alt=\"SMS inviati\"\\s*([^>]*)?>" +
      "<div\\s*([^>]*)?>\\s*([0-5])\\s*</div>";
  static final String RE_FORM_DATA =
      "<input\\s+([^>]*\\s+)?name=\"t:formdata\"\\s+([^>]*\\s+)?" +
      "value=\"([^\"]*)\"\\s*([^>]*)?>";
  static final String RE_RESULTS =
      "<span\\s*([^>]*)?>([0-9]{9,10})</span>" +
      "<span\\s*([^>]*)?>-</span>" +
      "<span\\s*([^>]*)?>([^<]*)?</span>";
  static final String RE_BLOCKING_ERROR =
      "<div\\s+([^>]*\\s+)?class=\"t-error\"\\s*([^>]*)?>" +
      "<div>Attenzione:</div><ul><li>([^<]*)</li></ul></div>";
  
  static final Pattern PATTERN_ADD_DISPATCH_NEW = Pattern.compile(
      "("+RE_LIMIT_ERROR+")|("+RE_GENERIC_ERROR+")|" +
      "("+RE_MESSAGE_COUNT+")|("+RE_FORM_DATA+")");
  static final Pattern PATTERN_ADD_DISPATCH_FORM = 
      Pattern.compile("("+RE_FORM_DATA+")");
  static final Pattern PATTERN_VALIDATE_CAPTCHA_FORM = Pattern.compile(
      "("+RE_GENERIC_ERROR+")|("+RE_BLOCKING_ERROR+")|("+RE_RESULTS+")");
  
  static final String LIMIT_ERROR = 
      "Oggi hai raggiunto il numero massimo di SMS gratis";
  static final String RECEIVER_ERROR = 
      "I seguenti numeri non sono corretti";
  static final String CAPTCHA_ERROR =
      "Le lettere che hai inserito non corrispondono " +
      "a quelle presenti nell'immagine";
  
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
  protected void initAccountConnector() {
    httpClient.getParams().setParameter(
        "http.protocol.allow-circular-redirects", true);
    httpClient.getParams().setParameter("http.useragent", 
        "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1)");
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
    
    } catch (SSLException s) {
      s.printStackTrace();
      s.getCause().printStackTrace();
      s.getCause().getCause().printStackTrace();
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
      
        boolean validateCaptchaForm = validateCaptchaForm(sms, results);
        if (!validateCaptchaForm) return results;

        updateCount();
        List<Receiver> sortedReceivers = new LinkedList<Receiver>();
        
        for (int r = 0; r < sms.getReceivers().size(); ++r)
          if (results.get(r).equals(Result.SUCCESSFUL)) {
            sortedReceivers.add(sms.getReceivers().get(r));
            ++count;
          }
        
        for (int r = 0; r < sms.getReceivers().size(); ++r)
          if (!results.get(r).equals(Result.SUCCESSFUL))
            sortedReceivers.add(sms.getReceivers().get(r));
        
        sms.setReceivers(sortedReceivers);
      }
      
    } catch (Exception e) {
      e.printStackTrace();
      results.set(0, Result.NETWORK_ERROR);
    }
    
    return results;
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

      Pattern noMore = Pattern.compile(RE_LIMIT_ERROR);
      for (String s : strings) {
        Matcher m = noMore.matcher(s);
        if (m.find() && m.group(3).contains(LIMIT_ERROR)) 
          return Result.LIMIT_ERROR;
      }
      
      Pattern error = Pattern.compile(RE_GENERIC_ERROR);
      for (String s : strings) {
        Matcher m = error.matcher(s);
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
      
      HttpPost request = new HttpPost(ADD_DISPATCH_FORM);
      List<NameValuePair> requestData = new ArrayList<NameValuePair>();
      requestData.add(new BasicNameValuePair("t:formdata", formData));
      requestData.add(new BasicNameValuePair("recipientType", "FREE_NUMBERS"));
      requestData.add(new BasicNameValuePair("t:formdata", separateFreeNumbers));
      requestData.add(new BasicNameValuePair("freeNumbers", freeNumbers));
      requestData.add(new BasicNameValuePair("t:formdata", ""));
      requestData.add(new BasicNameValuePair("contactListId", ""));
      requestData.add(new BasicNameValuePair("t:formdata", ""));
      requestData.add(new BasicNameValuePair("contactsIdString", ""));
      requestData.add(new BasicNameValuePair("deliverySmsClass", "STANDARD"));
      requestData.add(new BasicNameValuePair("textAreaStandard", sms.getMessage()));
      requestData.add(new BasicNameValuePair("textAreaFlash", ""));
      requestData.add(new BasicNameValuePair("modelsSelect", ""));
      request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.UTF_8));
      HttpResponse response = httpClient.execute(request, httpContext);
      List<String> strings = findPattern(
          response.getEntity().getContent(), PATTERN_ADD_DISPATCH_FORM);
      response.getEntity().consumeContent();
      
      formData = null;
      Pattern data = Pattern.compile(RE_FORM_DATA);
      for (String s : strings) {
        Matcher m = data.matcher(s);
        if (m.find()) formData = m.group(3);
      }

      if (formData == null)
        return Result.PROVIDER_ERROR;

      Result validateCaptchaImage = validateCaptchaImage(sms);
      if (validateCaptchaImage == Result.SUCCESSFUL)
        validateCaptchaImage = Result.CAPTCHA_NEEDED;
      return validateCaptchaImage;
      
    } catch (Exception e) {
      e.printStackTrace();
      return Result.NETWORK_ERROR;
    }
  }
  
  private Result validateCaptchaImage(SMS sms) 
      throws ClientProtocolException, IOException {
    HttpGet request = new HttpGet(VALIDATE_CAPTCHA_IMAGE);
    HttpResponse response = httpClient.execute(request, httpContext);
    if (response.getStatusLine().getStatusCode() != 200)
      return Result.PROVIDER_ERROR;
    
    sms.setCaptchaText("");
    sms.setCaptchaArray(toByteArray(response.getEntity().getContent()));
    response.getEntity().consumeContent();
    return Result.SUCCESSFUL;
  }
  
  private boolean validateCaptchaForm(SMS sms, List<Result> results) {
    try {
      
      HttpPost request = new HttpPost(VALIDATE_CAPTCHA_FORM);
      List<NameValuePair> requestData = new ArrayList<NameValuePair>();
      requestData.add(new BasicNameValuePair("t:ac", "Dispatch"));
      requestData.add(new BasicNameValuePair("t:formdata", formData));
      requestData.add(new BasicNameValuePair("verificationCode", sms.getCaptchaText()));
      request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.UTF_8));
      HttpResponse response = httpClient.execute(request, httpContext);
      List<String> strings = findPattern(
          response.getEntity().getContent(), PATTERN_VALIDATE_CAPTCHA_FORM);
      response.getEntity().consumeContent();

      Pattern error = Pattern.compile(RE_GENERIC_ERROR);
      for (String s : strings) {
        Matcher m = error.matcher(s);
        if (m.find()) {
          // Result.LIMIT_ERROR ?
          results.set(0, Result.UNKNOWN_ERROR);
          return false;
        }
      }
      
      Pattern captcha = Pattern.compile(RE_BLOCKING_ERROR);
      for (String s : strings) {
        Matcher m = captcha.matcher(s);
        if (m.find()) {
          String errorString = m.group(3);
          if (errorString.contains(RECEIVER_ERROR)) {
            results.set(0, Result.RECEIVER_ERROR);
          } else if (errorString.contains(CAPTCHA_ERROR)) {
            Result validateCaptchaImage = validateCaptchaImage(sms);
            if (validateCaptchaImage == Result.SUCCESSFUL)
              validateCaptchaImage = Result.CAPTCHA_ERROR;
            results.set(0, validateCaptchaImage);
          } else {
            results.set(0, Result.UNKNOWN_ERROR);
          }
          return false;
        }
      }

      boolean successful = false;
      Pattern completed = Pattern.compile(RE_RESULTS);
      for (String s : strings) {
        Matcher m = completed.matcher(s);
        if (m.find()) {
          successful = true;
          String number = m.group(2);
          for (int r = 0; r < sms.getReceivers().size(); ++r) {
            String receiver = sms.getReceivers().get(r).getNumber();
            if (receiver.contains(number)) {
              String what = m.group(5);
              if (what.equals("SMS inviato")) {
                results.set(r, Result.SUCCESSFUL);
              } else if (what.contains("il numero non è TIM")) {
                results.set(r, Result.RECEIVER_ERROR);
              }
            }
          }
        }
      }
      
      if (successful) return true;
      results.set(0, Result.UNKNOWN_ERROR);
      return false;
      
    } catch (Exception e) {
      e.printStackTrace();
      results.set(0, Result.NETWORK_ERROR);
      return false;
    }
  }
  
  /**
   * Remove country code prefix, if present.
   * @param receiver Phone number with or without CC prefix.
   * @return Phone number without CC prefix.
   */
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

  /**
   * Search for a pattern inside a stream.
   * @param source The stream to search into.
   * @param pattern The pattern to match.
   * @return List of matches found.
   */
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

  /**
   * Perform stream to array conversion.
   * @param is Stream to be converted.
   * @return An array of bytes.
   */
  private byte[] toByteArray(InputStream is) throws IOException {
    int read;
    byte[] data = new byte[16*1024]; // 16 kB
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    while ((read = is.read(data, 0, data.length)) != -1)
      buffer.write(data, 0, read);
    buffer.flush();
    return buffer.toByteArray();
  }
}
