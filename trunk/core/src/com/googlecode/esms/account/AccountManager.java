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
import java.util.List;

/**
 * Keep track of available providers and configured accounts.
 * @author Andrea De Pasquale
 */
public abstract class AccountManager implements Serializable {
  private static final long serialVersionUID = 1L;

  /**
   * Get all available providers.
   * @return list of providers
   */
  public abstract List<Account> getProviders();
  
  /**
   * Get all configured accounts.
   * @return list of accounts
   */
  public abstract List<Account> getAccounts();

  /**
   * Insert an account into the list of configured accounts.
   * @param newAccount account to insert
   */
  public abstract void insert(Account newAccount);
  
//  public abstract void update(Account account);
  
  /**
   * Delete an account from the list of configured accounts.
   * @param oldAccount account to delete
   */
  public abstract void delete(Account oldAccount);
}
