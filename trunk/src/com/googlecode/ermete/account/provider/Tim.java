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

import java.util.List;

import com.googlecode.ermete.account.Account;
import com.googlecode.ermete.account.AccountConnector;
import com.googlecode.ermete.sms.SMS;

public class Tim extends Account {
  private static final long serialVersionUID = 1L;

  static final String PROVIDER = "TIM";

  public Tim(AccountConnector connector) {
    super(connector);

    label = PROVIDER;
    provider = PROVIDER;
    limit = 5;
  }

  @Override
  public int calcRemaining(int length) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int calcFragments(int length) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Result login() {
    // TODO Auto-generated method stub
    return Result.LOGIN_ERROR;
  }

  @Override
  public Result logout() {
    // TODO Auto-generated method stub
    return Result.LOGOUT_ERROR;
  }
  
  @Override
  public List<String> getSenderList() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Result send(SMS sms) {
    // TODO Auto-generated method stub
    return Result.NETWORK_ERROR;
  }

}
