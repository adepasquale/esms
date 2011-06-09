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

import android.util.Log;

import com.googlecode.ermete.R;
import com.googlecode.ermete.account.Account;
import com.googlecode.ermete.account.AccountConnector;
import com.googlecode.ermete.sms.SMS;

// TODO web, widget and business all in one account
public class Vodafone extends Account {
  private static final long serialVersionUID = 1L;

  static final String PROVIDER = "Vodafone";

  public Vodafone(AccountConnector connector) {
    super(connector);

    httpClient.getParams().setParameter(
        "http.protocol.allow-circular-redirects", true);
    httpClient.getParams().setParameter("http.useragent", "Vodafone_DW");

    label = PROVIDER;
    provider = PROVIDER;
    logoID = R.drawable.ic_logo_vodafone;
    limit = 10;
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
      if (isLoggedIn())
        return Result.SUCCESSFUL;
      if (doLogin())
        return Result.SUCCESSFUL;
      else
        return Result.LOGIN_ERROR;
    } catch (Exception e) {
      e.printStackTrace();
      return Result.NETWORK_ERROR;
    }
  }

  @Override
  public String[] info() {
    // TODO Auto-generated method stub
    String[] senders = new String[2];
    senders[0] = "348 7654321";
    senders[1] = "340 1234567";
    return senders;
  }

  @Override
  public Result send(SMS sms) {
    // TODO Auto-generated method stub
    return Result.SUCCESSFUL;
  }

  private static final String CHECK_USER = "https://widget.vodafone.it/190/trilogy/jsp/utility/checkUser.jsp";

  private static final String DO_LOGIN = "https://widget.vodafone.it/190/trilogy/jsp/login.do";

  private boolean isLoggedIn() throws ClientProtocolException, IOException,
      IllegalStateException, JDOMException {
    HttpGet request = new HttpGet(CHECK_USER);
    Log.d("Vodafone", "request == null  -> " + (request == null));
    Log.d("Vodafone", "httpClient == null  -> " + (httpClient == null));
    Log.d("Vodafone", "httpContext == null  -> " + (httpContext == null));
    HttpResponse response = httpClient.execute(request, httpContext);
    Document document = new SAXBuilder().build(response.getEntity()
        .getContent());
    response.getEntity().consumeContent();
    Element root = document.getRootElement();
    Element child = root.getChild("logged-in");
    return child.getValue().equals("true");
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

    if (isLoggedIn())
      return true;
    return false;
  }

}
