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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.googlecode.awsms.R;
import com.googlecode.awsms.account.AccountManagerAndroid;
import com.googlecode.esms.account.Account;
import com.googlecode.esms.account.AccountManager;

/**
 * Choose a provider to create a new account. 
 * @author Andrea De Pasquale
 */
public class AccountCreateActivity extends Activity {

  AccountManager accountManager;

  LinearLayout providersLinear;
  Button prevButton, nextButton;
  
  boolean[] checked;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.account_create_activity);

    accountManager = new AccountManagerAndroid(AccountCreateActivity.this);
    
    providersLinear = (LinearLayout) findViewById(R.id.providers_linear);
    for (final Account provider : accountManager.getProviders()) {

      View listItem = getLayoutInflater().inflate(
          R.layout.account_create_list_item, null);
      LinearLayout listItemLinear = (LinearLayout) listItem
          .findViewById(R.id.list_item_linear);
      TextView listItemProvider = (TextView) listItem
          .findViewById(R.id.list_item_provider);
      ImageView listItemLogo = (ImageView) listItem
          .findViewById(R.id.list_item_logo);

      listItemProvider.setText(provider.getProvider());
      
      listItemLogo.setImageBitmap(
          AccountBitmap.getLogo(provider, getResources()));
      
      listItemLinear.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          for (int i = 0; i < providersLinear.getChildCount(); ++i) {
            View listItem = providersLinear.getChildAt(i);
            CheckedTextView listItemProvider = (CheckedTextView) 
                listItem.findViewById(R.id.list_item_provider);
            listItemProvider.setChecked(false);
          }
          
          CheckedTextView listItemProvider = 
              (CheckedTextView) v.findViewById(R.id.list_item_provider);
          listItemProvider.toggle();
          nextButton.setEnabled(true);
        }
      });

      providersLinear.addView(listItem);
    }
    
    prevButton = (Button) findViewById(R.id.prev_button);
    prevButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        AccountCreateActivity.this.finish();
      }
    });
    
    nextButton = (Button) findViewById(R.id.next_button);
    nextButton.setEnabled(false);
    nextButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Account provider = null;
        for (int i = 0; i < providersLinear.getChildCount(); ++i) {
          View listItem = providersLinear.getChildAt(i);
          CheckedTextView listItemProvider = (CheckedTextView) 
              listItem.findViewById(R.id.list_item_provider);
          if (listItemProvider.isChecked()) 
            provider = accountManager.getProviders().get(i);
        }
        
        Intent intent = new Intent(
            AccountCreateActivity.this,
            AccountModifyActivity.class);
        intent.setAction(AccountIntents.DO_AUTHENTICATION);
        intent.putExtra(AccountIntents.NEW_ACCOUNT, provider);
        startActivity(intent);
      }
    });
  }
}
