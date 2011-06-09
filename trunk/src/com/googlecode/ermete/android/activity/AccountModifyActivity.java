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

package com.googlecode.ermete.android.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
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

import com.googlecode.ermete.R;
import com.googlecode.ermete.account.Account;
import com.googlecode.ermete.account.AccountConnectorAndroid;
import com.googlecode.ermete.account.AccountManager;
import com.googlecode.ermete.account.AccountManagerAndroid;

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
    final Account account = (Account) intent
        .getSerializableExtra("com.googlecode.ermete.ACCOUNT");
    account.setAccountConnector(new AccountConnectorAndroid(this));

    titleLogo.setImageBitmap(BitmapFactory.decodeResource(getResources(),
        account.getLogoID()));

    titleText.setText(getString(R.string.account_modify_activity) + " "
        + account.getLabel());

    if (action.equals("com.googlecode.ermete.DO_AUTHENTICATION")) {
      loginLinear.setVisibility(View.VISIBLE);

      usernameText.setText(account.getUsername());
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

      passwordText.setText(account.getPassword());
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

      toggleNextButton(usernameText.length(), passwordText.length());
      nextButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          new AsyncTask<Void, Void, Account.Result>() {
            ProgressDialog progress;

            protected void onPreExecute() {
              progress = ProgressDialog.show(AccountModifyActivity.this, "",
                  getString(R.string.login_progress_dialog), true, false);
            }

            protected Account.Result doInBackground(Void... params) {
              try {
                Thread.sleep(2500);
              } catch (InterruptedException e) {
              }
              return account.login();
            }

            protected void onPostExecute(Account.Result result) {
              progress.dismiss();

              switch (result) {
              case SUCCESSFUL:
                Intent intent = new Intent(AccountModifyActivity.this,
                    AccountModifyActivity.class);
                intent.setAction("com.googlecode.ermete.CHOOSE_SENDER");
                intent.putExtra("com.googlecode.ermete.ACCOUNT", account);
                startActivity(intent);
                break;

              case LOGIN_ERROR:
                Toast.makeText(AccountModifyActivity.this,
                    R.string.login_error_toast, Toast.LENGTH_LONG).show();
                break;

              default:
                Toast.makeText(AccountModifyActivity.this,
                    R.string.network_error_toast, Toast.LENGTH_LONG).show();
              }
            }

          }.execute();
        }
      });
    }

    if (action.equals("com.googlecode.ermete.CHOOSE_SENDER")) {
      senderLinear.setVisibility(View.VISIBLE);

      senderRadio.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        public void onCheckedChanged(RadioGroup group, int checkedId) {
          nextButton.setEnabled(true);
        }
      });

      nextButton.setEnabled(false);
      nextButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          Intent intent = new Intent(AccountModifyActivity.this,
              AccountModifyActivity.class);
          intent.setAction("com.googlecode.ermete.CHOOSE_LABEL");
          intent.putExtra("com.googlecode.ermete.ACCOUNT", account);
          startActivity(intent);
        }
      });

      float scale = getResources().getDisplayMetrics().density;
      int padding = (int) (12 * scale + 0.5f);

      for (String sender : account.info()) {
        RadioButton rb = new RadioButton(this);
        rb.setText(sender);
        rb.setPadding(4 * padding, padding, 0, padding);
        senderRadio.addView(rb);
        if (sender.equals(account.getSender()))
          rb.setChecked(true);
      }
    }

    if (action.equals("com.googlecode.ermete.CHOOSE_LABEL")) {
      labelLinear.setVisibility(View.VISIBLE);

      labelText.setText(account.getLabel());
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

      nextButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          Intent intent = new Intent(AccountModifyActivity.this,
              AccountDisplayActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivity(intent);
        }
      });
    }

    prevButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        AccountModifyActivity.this.finish();
      }
    });
  }

  private void toggleNextButton(int uLength, int pLength) {
    if (uLength == 0 || pLength == 0)
      nextButton.setEnabled(false);
    else
      nextButton.setEnabled(true);
  }

}
