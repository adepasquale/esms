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

package com.googlecode.awsms.android;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.googlecode.awsms.R;
import com.googlecode.esms.account.Account;

/**
 * Handle graphical resources for each account. 
 * @author Andrea De Pasquale
 */
public class AccountBitmap {
  
  /**
   * Get resource bitmap logo for a given account.
   * @param account Ermete SMS account or provider.
   * @param resources Android application resources.
   * @return Bitmap logo for the account.
   */
  static public Bitmap getLogo(Account account, Resources resources) {
//    if (provider.getProvider().equals("Telefono")) {
//      return BitmapFactory.decodeResource(
//          resources, R.drawable.ic_logo_SIM);
//    }
    
    if (account.getProvider().equals("Vodafone")) {
      return BitmapFactory.decodeResource(
          resources, R.drawable.ic_logo_vodafone);
    }
      
    if (account.getProvider().equals("TIM")) {
      return BitmapFactory.decodeResource(
          resources, R.drawable.ic_logo_tim);
    }
    
    return null;
  }
}

