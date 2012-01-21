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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.awsms.R;
import com.googlecode.awsms.account.AccountConnectorAndroid;
import com.googlecode.awsms.account.AccountManagerAndroid;
import com.googlecode.esms.account.Account;
import com.googlecode.esms.account.AccountManager;

public class AccountOverviewActivity extends Activity {
  
  AccountManager accountManager;
  boolean modified;
  
  ImageView titleLogo;
  TextView titleText;
  TextView overviewText;
  
  Button endButton;
  Button createButton;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.account_overview_activity);
    
    accountManager = new AccountManagerAndroid(AccountOverviewActivity.this);

    titleLogo = (ImageView) findViewById(R.id.title_logo);
    titleText = (TextView) findViewById(R.id.title_text);
    overviewText = (TextView) findViewById(R.id.overview_text);
    endButton = (Button) findViewById(R.id.end_button);
    createButton = (Button) findViewById(R.id.create_button);
    
    Intent intent = getIntent();
    Account account = (Account) 
        intent.getSerializableExtra(AccountIntents.NEW_ACCOUNT);
    account.setAccountConnector(new AccountConnectorAndroid(this));
    modified = intent.getBooleanExtra(AccountIntents.ACTION_MODIFY, false);

//    if (account.getProvider().equals("Telefono")) {
//      titleLogo.setImageBitmap(
//          BitmapFactory.decodeResource(getResources(),
//          R.drawable.ic_logo_SIM));
//    }
    
    if (account.getProvider().equals("Vodafone")) {
      titleLogo.setImageBitmap(
          BitmapFactory.decodeResource(getResources(),
          R.drawable.ic_logo_vodafone));
    }
      
    if (account.getProvider().equals("TIM")) { 
      titleLogo.setImageBitmap(
          BitmapFactory.decodeResource(getResources(), 
          R.drawable.ic_logo_tim));
    }
    
    titleText.setText(
        getString(R.string.account_text) + 
        " " + account.getLabel());
    
    String action = getString(R.string.overview_create);
    if (modified) action = getString(R.string.overview_modify); 
    
    overviewText.setText(String.format(
        getString(R.string.overview_text), 
        account.getLabel(), action));
    
    createButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(
            AccountOverviewActivity.this,
            AccountCreateActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);        
      }
    });
    
    endButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent;
        if (modified) {
          intent = new Intent(
              AccountOverviewActivity.this,
              AccountDisplayActivity.class);
        } else {
          intent = new Intent(
              AccountOverviewActivity.this,
              ComposeActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
      }
    });
  }
  
  @Override
  public void onBackPressed() {
    Intent intent;
    if (modified) {
      intent = new Intent(
          AccountOverviewActivity.this,
          AccountDisplayActivity.class);
    } else {
      intent = new Intent(
          AccountOverviewActivity.this,
          ComposeActivity.class);
    }
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(intent);
  }
}
