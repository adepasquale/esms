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
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.awsms.account.AccountConnectorAndroid;
import com.googlecode.awsms.account.AccountManagerAndroid;
import com.googlecode.awsms.R;
import com.googlecode.esms.account.Account;
import com.googlecode.esms.account.AccountManager;

public class AccountModifyActivity extends Activity {

  AccountManager accountManager;

  ImageView titleLogo;
  TextView titleText;
  LinearLayout loginLinear;
  EditText usernameText;
  EditText passwordText;
  LinearLayout senderLinear;
  RadioGroup senderRadio;
  LinearLayout labelLinear;
  EditText labelText;
  Button prevButton;
  Button nextButton;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.account_modify_activity);

    accountManager = new AccountManagerAndroid(AccountModifyActivity.this);

    titleLogo = (ImageView) findViewById(R.id.title_logo);
    titleText = (TextView) findViewById(R.id.title_text);
    loginLinear = (LinearLayout) findViewById(R.id.login_linear);
    usernameText = (EditText) findViewById(R.id.username_text);
    passwordText = (EditText) findViewById(R.id.password_text);
    senderLinear = (LinearLayout) findViewById(R.id.sender_linear);
    senderRadio = (RadioGroup) findViewById(R.id.sender_radio);
    labelLinear = (LinearLayout) findViewById(R.id.label_linear);
    labelText = (EditText) findViewById(R.id.label_text);
    prevButton = (Button) findViewById(R.id.prev_button);
    nextButton = (Button) findViewById(R.id.next_button);

    Intent intent = getIntent();
    String action = intent.getAction();
    
    final Account newAccount = (Account) 
        intent.getSerializableExtra(AccountIntents.NEW_ACCOUNT);
    final Account oldAccount = (Account)
        intent.getSerializableExtra(AccountIntents.OLD_ACCOUNT);

    newAccount.setAccountConnector(new AccountConnectorAndroid(this));
    
//    if (newAccount.getProvider().equals("Telefono")) {
//      titleLogo.setImageBitmap(
//          BitmapFactory.decodeResource(getResources(),
//          R.drawable.ic_logo_SIM));
//    }
    
    if (newAccount.getProvider().equals("Vodafone")) {
      titleLogo.setImageBitmap(
          BitmapFactory.decodeResource(getResources(),
          R.drawable.ic_logo_vodafone));
    }
      
    if (newAccount.getProvider().equals("TIM")) { 
      titleLogo.setImageBitmap(
          BitmapFactory.decodeResource(getResources(), 
          R.drawable.ic_logo_tim));
    }
    
    titleText.setText(getString(R.string.account_text) + 
        " " + newAccount.getLabel());

    if (action.equals(AccountIntents.DO_AUTHENTICATION)) {
      loginLinear.setVisibility(View.VISIBLE);

      // import old username and password from Android Web SMS
      SharedPreferences prefs = PreferenceManager
          .getDefaultSharedPreferences(AccountModifyActivity.this);
      
      String username = newAccount.getUsername();
      if (oldAccount == null && newAccount.getProvider().equals("Vodafone"))
        username = prefs.getString("VodafoneItalyUsername", username);
      usernameText.setText(username);
//      usernameText.setText(newAccount.getUsername());
      usernameText.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
        }

        public void afterTextChanged(Editable s) {
        }

        public void onTextChanged(CharSequence s, int start, int before,
            int count) {
          toggleNextButton(s.length(), passwordText.length());
        }
      });

      String password = newAccount.getPassword();
      if (oldAccount == null && newAccount.getProvider().equals("Vodafone"))
        password = prefs.getString("VodafoneItalyPassword", password);
      passwordText.setText(password);
//      passwordText.setText(newAccount.getPassword());
      passwordText.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
        }

        public void afterTextChanged(Editable s) {
        }

        public void onTextChanged(CharSequence s, int start, int before,
            int count) {
          toggleNextButton(usernameText.length(), s.length());
        }
      });

      prevButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          AccountModifyActivity.this.finish();
        }
      });
      
      toggleNextButton(usernameText.length(), passwordText.length());
      nextButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          new AsyncTask<Void, Void, Account.Result>() {
            ProgressDialog progress;

            protected void onPreExecute() {
              progress = ProgressDialog.show(AccountModifyActivity.this, "",
                  getString(R.string.login_progress_dialog), true, false);
            }

            protected Account.Result doInBackground(Void... params) {
              newAccount.setUsername(usernameText.getText().toString());
              newAccount.setPassword(passwordText.getText().toString());
              return newAccount.login();
            }

            protected void onPostExecute(Account.Result result) {
              progress.dismiss();

              switch (result) {
              case SUCCESSFUL:
                Intent intent = new Intent(AccountModifyActivity.this,
                    AccountModifyActivity.class);
                intent.setAction(AccountIntents.CHOOSE_SENDER);
                intent.putExtra(AccountIntents.NEW_ACCOUNT, newAccount);
                if (oldAccount != null) 
                  intent.putExtra(AccountIntents.OLD_ACCOUNT, oldAccount);
                startActivity(intent);
                break;

              case LOGIN_ERROR:
                Toast.makeText(AccountModifyActivity.this,
                    R.string.login_error_toast, Toast.LENGTH_LONG).show();
                break;

              case LOGOUT_ERROR:
                Toast.makeText(AccountModifyActivity.this,
                    R.string.logout_error_toast, Toast.LENGTH_LONG).show();
                break;

              case UNSUPPORTED_ERROR:
                Toast.makeText(AccountModifyActivity.this,
                    R.string.unsupported_error_toast, Toast.LENGTH_LONG).show();
                break;
                
              case PROVIDER_ERROR:
                Toast.makeText(AccountModifyActivity.this,
                    R.string.provider_error_toast, Toast.LENGTH_LONG).show();
                break;
                
              case NETWORK_ERROR:
              default:
                Toast.makeText(AccountModifyActivity.this,
                    R.string.network_error_toast, Toast.LENGTH_LONG).show();
              }
            }

          }.execute();
        }
      });
    }

    if (action.equals(AccountIntents.CHOOSE_SENDER)) {
      senderLinear.setVisibility(View.VISIBLE);

      senderRadio.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        public void onCheckedChanged(RadioGroup group, int checkedId) {
          nextButton.setEnabled(true);
        }
      });

      prevButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          new AsyncTask<Void, Void, Account.Result>() {
            ProgressDialog progress;

            protected void onPreExecute() {
              progress = ProgressDialog.show(AccountModifyActivity.this, "",
                  getString(R.string.logout_progress_dialog), true, false);
            }

            protected Account.Result doInBackground(Void... params) {
              return newAccount.logout();
            }

            protected void onPostExecute(Account.Result result) {
              progress.dismiss();

              switch (result) {
              case SUCCESSFUL:
                Intent intent = new Intent(AccountModifyActivity.this,
                    AccountModifyActivity.class);
                intent.setAction(AccountIntents.DO_AUTHENTICATION);
                intent.putExtra(AccountIntents.NEW_ACCOUNT, newAccount);
                if (oldAccount != null) 
                  intent.putExtra(AccountIntents.OLD_ACCOUNT, oldAccount);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;

              case LOGOUT_ERROR:
                Toast.makeText(AccountModifyActivity.this,
                    R.string.logout_error_toast, Toast.LENGTH_LONG).show();
                break;

              case PROVIDER_ERROR:
                Toast.makeText(AccountModifyActivity.this,
                    R.string.provider_error_toast, Toast.LENGTH_LONG).show();
                break;
                
              case NETWORK_ERROR:
              default:
                Toast.makeText(AccountModifyActivity.this,
                    R.string.network_error_toast, Toast.LENGTH_LONG).show();
              }
            }

          }.execute();
        }
      });
      
      nextButton.setEnabled(false);
      nextButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          Intent intent = new Intent(AccountModifyActivity.this,
              AccountModifyActivity.class);
          intent.setAction(AccountIntents.CHOOSE_LABEL);
          int checkedId = senderRadio.getCheckedRadioButtonId();
          RadioButton rb = (RadioButton) senderRadio.findViewById(checkedId);
          newAccount.setSender(rb.getText().toString());
          intent.putExtra(AccountIntents.NEW_ACCOUNT, newAccount);
          if (oldAccount != null) 
            intent.putExtra(AccountIntents.OLD_ACCOUNT, oldAccount);
          startActivity(intent);
        }
      });

      float scale = getResources().getDisplayMetrics().density;
      int padding = (int) (12 * scale + 0.5f);

      for (String sender : newAccount.getSenderList()) {
        RadioButton rb = new RadioButton(this);
        rb.setText(sender);
        rb.setPadding(4 * padding, padding, 0, padding);
        senderRadio.addView(rb);
        if (sender.equals(newAccount.getSender()))
          rb.setChecked(true);
      }
    }

    if (action.equals(AccountIntents.CHOOSE_LABEL)) {
      labelLinear.setVisibility(View.VISIBLE);

      String label = newAccount.getLabel();
      if (oldAccount == null) {
        int suffix = 1;
        for (Account account : accountManager.getAccounts())
          if (account.getLabel().equalsIgnoreCase(label)) ++suffix;
        if (suffix > 1) label += " (" + suffix + ")";
      }
      labelText.setText(label);
      labelText.setSelection(0, labelText.length());
      labelText.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
        }

        public void afterTextChanged(Editable s) {
        }

        public void onTextChanged(CharSequence s, int start, int before,
            int count) {
          if (s.length() > 0)
            nextButton.setEnabled(true);
          else
            nextButton.setEnabled(false);
        }
      });

      prevButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          Intent intent = new Intent(AccountModifyActivity.this,
              AccountModifyActivity.class);
          intent.setAction(AccountIntents.CHOOSE_SENDER);
          intent.putExtra(AccountIntents.NEW_ACCOUNT, newAccount);
          if (oldAccount != null) 
            intent.putExtra(AccountIntents.OLD_ACCOUNT, oldAccount);
          startActivity(intent);
        }
      });
      
      nextButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          AccountManager accountManager = 
            new AccountManagerAndroid(AccountModifyActivity.this);
          if (oldAccount != null) accountManager.delete(oldAccount);
          
          String label = labelText.getText().toString();
          for (Account account : accountManager.getAccounts())
            if (account.getLabel().equalsIgnoreCase(label)) {
              if (oldAccount != null) accountManager.insert(oldAccount);
              Toast.makeText(AccountModifyActivity.this, 
                  R.string.existing_label_toast, Toast.LENGTH_SHORT).show();
              return;
            }

          newAccount.setLabel(label);
          accountManager.insert(newAccount);
          
          if (oldAccount == null && newAccount.getProvider().equals("Vodafone")) {
            SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(AccountModifyActivity.this).edit();
            editor.remove("VodafoneItalyUsername");
            editor.remove("VodafoneItalyPassword");
            editor.commit();
          }
          
          Intent intent = new Intent(AccountModifyActivity.this,
              AccountOverviewActivity.class);
          intent.putExtra(AccountIntents.NEW_ACCOUNT, newAccount);
          startActivity(intent);
//          intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
      });
    }
  }

  private void toggleNextButton(int uLength, int pLength) {
    if (uLength == 0 || pLength == 0)
      nextButton.setEnabled(false);
    else
      nextButton.setEnabled(true);
  }

}
