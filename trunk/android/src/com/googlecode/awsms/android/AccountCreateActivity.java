/*
 *  This file is part of Ermete SMS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.googlecode.awsms.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.googlecode.awsms.account.AccountManagerAndroid;
import com.googlecode.awsms.R;
import com.googlecode.esms.account.Account;
import com.googlecode.esms.account.AccountManager;

public class AccountCreateActivity extends Activity {

  AccountManager accountManager;

  LinearLayout providersLinear;

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
      
      if (provider.getProvider().equals("Vodafone"))
        listItemLogo.setImageBitmap(
            BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_logo_vodafone));
        
      if (provider.getProvider().equals("TIM"))
        listItemLogo.setImageBitmap(
            BitmapFactory.decodeResource(getResources(), 
            R.drawable.ic_logo_tim));
      
      if (provider.getProvider().equals("3"))
        listItemLogo.setImageBitmap(
            BitmapFactory.decodeResource(getResources(), 
            R.drawable.ic_logo_tim)); // XXX logo 3

      listItemLinear.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          Intent intent = new Intent(AccountCreateActivity.this,
              AccountModifyActivity.class);
          intent.setAction(AccountIntents.DO_AUTHENTICATION);
          intent.putExtra(AccountIntents.NEW_ACCOUNT, provider);
          startActivity(intent);
        }
      });

      providersLinear.addView(listItem);
    }
  }
}
