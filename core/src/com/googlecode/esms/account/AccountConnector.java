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

import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;

/**
 * Container for HTTP connection classes, needed by Account. 
 * @author Andrea De Pasquale
 */
public abstract class AccountConnector implements Serializable {
  private static final long serialVersionUID = 1L;

  public abstract HttpClient getHttpClient();

  public abstract HttpContext getHttpContext();

  public abstract CookieStore getCookieStore();

}
