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

package com.googlecode.ermete.account;

import java.util.LinkedList;

import android.content.Context;

import com.googlecode.ermete.R;
import com.googlecode.ermete.account.provider.Tim;
import com.googlecode.ermete.account.provider.Vodafone;

public class AccountManagerAndroid extends AccountManager {
  private static final long serialVersionUID = 1L;

  public AccountManagerAndroid(Context context) {
    AccountConnectorAndroid connector = new AccountConnectorAndroid(context);

    providers = new LinkedList<Account>();
    providers.add(new Vodafone(connector));
    providers.add(new Tim(connector));

    accounts = new LinkedList<Account>();
    // TODO load accounts from the DB
    Account test1 = new Vodafone(connector);
    test1.setLabel("Lavoro V.");
    test1.setSender("340 1234567");
    test1.setUsername("vodafone");
    test1.setPassword("password");
    test1.setLogoID(R.drawable.ic_logo_vodafone);
    accounts.add(test1);
    Account test2 = new Tim(connector);
    test2.setLabel("Personale T.");
    test2.setSender("333 1234567");
    test2.setUsername("telecom");
    test2.setPassword("password");
    test2.setLogoID(R.drawable.ic_logo_vodafone);
    accounts.add(test2);
  }

  @Override
  public void insert(Account account) {
    // TODO Auto-generated method stub
    accounts.add(account);
  }

  @Override
  public void modify(int index) {
    // TODO Auto-generated method stub
  }

  @Override
  public void delete(int index) {
    // TODO Auto-generated method stub
    accounts.remove(index);
  }

}
