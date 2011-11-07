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

package com.googlecode.awsms.provider;

import java.util.List;

import com.googlecode.esms.account.Account;
import com.googlecode.esms.message.SMS;

/**
 * Default account to use SIM messaging service. 
 * @author Andrea De Pasquale
 */
public class Telephony extends Account {
  private static final long serialVersionUID = 1L;
  
  static final String PROVIDER = "Telefono";
  
  public Telephony() {
    super(null);
    
    label = PROVIDER;
    provider = PROVIDER;
    limit = 0;
  }
  
  @Override
  public int calcRemaining(int length) {
    // TODO use same method of default app
    return 0;
  }

  @Override
  public int calcFragments(int length) {
    // TODO use same method of default app
    return 0;
  }

  @Override
  public List<String> getSenderList() {
    // TODO retrieve SIM phone number
    return null;
  }

  @Override
  public Result login() {
    return Result.SUCCESSFUL;
  }

  @Override
  public Result logout() {
    return Result.SUCCESSFUL;
  }

  @Override
  public Result send(SMS sms) {
    // TODO use telephony provider
    return Result.UNSUPPORTED_ERROR;
  }

}
