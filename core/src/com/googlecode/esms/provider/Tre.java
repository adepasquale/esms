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

import java.util.List;

import com.googlecode.esms.account.Account;
import com.googlecode.esms.account.AccountConnector;
import com.googlecode.esms.message.SMS;

// TODO goText open source project on SourceForge
public class Tre extends Account {
  private static final long serialVersionUID = 1L;

  static final String PROVIDER = "3";

  public Tre(AccountConnector connector) {
    super(connector);

    label = PROVIDER;
    provider = PROVIDER;
    limit = 0;
  }

  @Override
  public int calcRemaining(int length) {
    return 0;
  }

  @Override
  public int calcFragments(int length) {
    return 0;
  }
  
  @Override
  protected void updateCount() {
    return;
  }

  @Override
  public Result login() {
    return Result.UNSUPPORTED_ERROR;
  }

  @Override
  public Result logout() {
    return Result.UNSUPPORTED_ERROR;
  }
  
  @Override
  public List<String> getSenderList() {
    return null;
  }

  @Override
  public Result send(SMS sms) {
    return Result.UNSUPPORTED_ERROR;
  }

}
