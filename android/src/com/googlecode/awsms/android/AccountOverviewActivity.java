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

/**
 * Confirm successful account creation or modification.
 * @author Andrea De Pasquale
 */
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
