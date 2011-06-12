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

package com.googlecode.ermete.android.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Annotation;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.ermete.R;
import com.googlecode.ermete.account.Account;
import com.googlecode.ermete.account.AccountManager;
import com.googlecode.ermete.account.AccountManagerAndroid;
import com.googlecode.ermete.android.ReceiverAdapter;
import com.googlecode.ermete.android.service.AccountService;
import com.googlecode.ermete.android.service.AccountService.AccountServiceBinder;
import com.googlecode.ermete.sms.ConversationManager;
import com.googlecode.ermete.sms.ConversationManagerAndroid;
import com.googlecode.ermete.sms.SMS;

public class ComposeActivity extends Activity {

  ConversationManager conversationManager;
  AccountManager accountManager;
  Account account;

  SharedPreferences preferences;

  ServiceConnection serviceConnection;
  AccountService accountService;
  boolean serviceBound;

  ImageView counterImage;
  TextView counterText;

  Spinner senderSpinner;
  ImageButton clearButton;

  AutoCompleteTextView receiverText;
  String receiverIncomplete;
  boolean hasAutocompleted;
  ImageButton plusButton;

  LinearLayout listLinear;
  int listSize;

  LinearLayout replyLinear;
  TextView replyContent;
  TextView replyDate;

  EditText messageText;
  Button sendButton;
  TextView lengthText;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.compose_activity);

    conversationManager = new ConversationManagerAndroid();

    serviceConnection = new ServiceConnection() {
      public void onServiceConnected(ComponentName name, IBinder service) {
        AccountServiceBinder binder = (AccountServiceBinder) service;
        accountService = binder.getService();
      }

      public void onServiceDisconnected(ComponentName name) {
        accountService = null;
      }
    };

    if (bindService(new Intent(ComposeActivity.this, AccountService.class),
        serviceConnection, Context.BIND_AUTO_CREATE)) {
      serviceBound = true;
    }

    preferences = PreferenceManager.getDefaultSharedPreferences(this);

    clearButton = (ImageButton) findViewById(R.id.clear_button);
    clearButton.setEnabled(false);
    clearButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        clearButton.setEnabled(false);
        receiverText.setText("");
        receiverIncomplete = "";
        hasAutocompleted = false;
        listLinear.setVisibility(View.GONE);
        listLinear.removeViews(0, listSize);
        listSize = 0;
        replyLinear.setVisibility(View.VISIBLE);
        replyContent.setText("");
        replyDate.setText("");
        messageText.setText("");
        lengthText.setVisibility(View.GONE);
        lengthText.setText("");
        sendButton.setEnabled(false);
      }
    });

    receiverText = (AutoCompleteTextView) findViewById(R.id.receiver);
    receiverText.setAdapter(new ReceiverAdapter(this));
    receiverText.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        TextView receiverName = (TextView) view
            .findViewById(R.id.receiver_name);
        TextView receiverNumber = (TextView) view
            .findViewById(R.id.receiver_number);

        hasAutocompleted = true;

        if (listSize == 0) {
          SMS reply = conversationManager.loadInbox(receiverNumber.getText()
              .toString());
          replyContent.setText(reply.getMessage());
          replyDate.setText(reply.getDate());
        } else {
          addListItem(receiverName.getText().toString(), receiverNumber
              .getText().toString());
          receiverText.setText("");
        }

        toggleButtons(receiverText.getText().length(), messageText.getText()
            .length());
      }
    });
    receiverText.addTextChangedListener(new TextWatcher() {
      public void beforeTextChanged(CharSequence s, int start, int count,
          int after) {
      }

      public void afterTextChanged(Editable s) {
      }

      public void onTextChanged(CharSequence s, int start, int before, int count) {
        toggleButtons(s.length(), messageText.length());

        // deleting one char
        if (before == 1 && count == 0) {
          if (hasAutocompleted) {
            receiverText.setText("");
            receiverText.append(receiverIncomplete);
            hasAutocompleted = false;
          } else {
            receiverIncomplete = s.toString();
          }
        }

        // writing one char
        if (before == 0 && count == 1) {
          receiverIncomplete = s.toString();
          hasAutocompleted = false;
        }

        if (s.length() == 0) {
          replyContent.setText("");
          replyDate.setText("");
        }
      }
    });

    plusButton = (ImageButton) findViewById(R.id.plus_button);
    plusButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        String name = "";
        String number = receiverText.getText().toString();

        Spannable s = receiverText.getText();
        Annotation[] annotations = s.getSpans(0, s.length(), Annotation.class);
        for (Annotation a : annotations) {
          if (a.getKey().equals("name"))
            name = a.getValue();
          if (a.getKey().equals("number"))
            number = a.getValue();
        }

        number = number.replaceAll("[^0-9\\+]*", "");
        if (number.equals(""))
          return;

        addListItem(name, number);
        receiverText.setText("");
        receiverIncomplete = "";
        hasAutocompleted = false;
      }
    });

    listLinear = (LinearLayout) findViewById(R.id.list_linear);
    listSize = 0;

    replyLinear = (LinearLayout) findViewById(R.id.reply_linear);
    replyContent = (TextView) findViewById(R.id.reply_content);
    replyDate = (TextView) findViewById(R.id.reply_date);

    messageText = (EditText) findViewById(R.id.message);
    messageText.addTextChangedListener(new TextWatcher() {
      public void beforeTextChanged(CharSequence s, int start, int count,
          int after) {
      }

      public void afterTextChanged(Editable s) {
      }

      public void onTextChanged(CharSequence s, int start, int before, int count) {
        int l = s.toString().replaceAll("\\s+$", "").replaceAll("\\s{2,}", " ")
            .length();

        int r = account.calcRemaining(l);
        int f = account.calcFragments(l);

        if (f > 1 || (f == 1 && r < 20)) {
          lengthText.setVisibility(View.VISIBLE);
          lengthText.setText(r + " / " + f);
        } else {
          if (messageText.getLineCount() > 3)
            lengthText.setVisibility(View.INVISIBLE);
          else
            lengthText.setVisibility(View.GONE);
          lengthText.setText("");
        }

        toggleButtons(receiverText.getText().length(), l);
      }
    });

    sendButton = (Button) findViewById(R.id.send);
    sendButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        String receiverName[], receiverNumber[];
        if (listSize == 0) {
          receiverName = new String[1];
          receiverNumber = new String[1];
          receiverNumber[0] = receiverText.getText().toString();
          Spannable s = receiverText.getText();
          Annotation[] annotations = s.getSpans(0, s.length(), Annotation.class);
          for (Annotation a : annotations) {
            if (a.getKey().equals("name"))
              receiverName[0] = a.getValue();
            if (a.getKey().equals("number"))
              receiverNumber[0] = a.getValue();
          }
        } else {
          receiverName = new String[listSize];
          receiverNumber = new String[listSize];
          for (int i = 0; i < listSize; ++i) {
            View listItem = listLinear.getChildAt(i);
            TextView listItemName = (TextView) listItem
                .findViewById(R.id.list_item_name);
            TextView listItemNumber = (TextView) listItem
                .findViewById(R.id.list_item_number);
            receiverName[i] = listItemName.getText().toString();
            receiverNumber[i] = listItemNumber.getText().toString();
          }
        }

        SMS sms = new SMS(messageText.getText().toString());
        sms.setReceiverName(receiverName);
        sms.setReceiverNumber(receiverNumber);
        if (accountService != null)
          accountService.send(ComposeActivity.this, account, sms);

        if (!preferences.getBoolean("show_progress", false)) {
          Toast.makeText(ComposeActivity.this, R.string.sending_toast,
              Toast.LENGTH_LONG).show();

          // disable the button and re-enable it after some time
          sendButton.setEnabled(false);
          new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
              try {
                Thread.sleep(3500);
              } catch (InterruptedException e) {
              }
              return null;
            }

            protected void onPostExecute(Void result) {
              sendButton.setEnabled(true);
            };
          }.execute();
        }

        clearFields();
      }
    });

    lengthText = (TextView) findViewById(R.id.length);
  }

  @Override
  public void onResume() {
    super.onResume();
    
    accountManager = new AccountManagerAndroid(ComposeActivity.this);
    if (accountManager.getAccounts().size() == 0)
      startActivity(new Intent(this, AccountDisplayActivity.class));
    
    counterImage = (ImageView) findViewById(R.id.counter_image);
    counterText = (TextView) findViewById(R.id.counter_text);

    ArrayList<CharSequence> providers = new ArrayList<CharSequence>();
    for (Account account : accountManager.getAccounts()) {
      String label = account.getLabel();
      if (label == null || label.equals(""))
        label = getString(R.string.no_label_text);
      providers.add(label);
    }
    ArrayAdapter<CharSequence> senderAdapter = new ArrayAdapter<CharSequence>(
        this, android.R.layout.simple_spinner_item, providers);
    senderAdapter
        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    senderSpinner = (Spinner) findViewById(R.id.sender_spinner);
    senderSpinner.setAdapter(senderAdapter);
    senderSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onNothingSelected(AdapterView<?> parent) { }
      public void onItemSelected(AdapterView<?> parent, View view,
          int position, long id) {
        List<Account> accounts = accountManager.getAccounts();
        account = accounts.get(position);
        int remaining = account.getLimit() - account.getCount();
        counterText.setText(Integer.toString(remaining));
        if (accountService != null)
          accountService.login(account);
      }
    });
  }
  
  @Override
  public void onDestroy() {
    super.onDestroy();
    if (serviceBound) {
      unbindService(serviceConnection);
      serviceBound = false;
    }
  }

  @Override
  public void onBackPressed() {
    // prevent onDestroy() call
    moveTaskToBack(true);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.layout.compose_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.menu_item_accounts:
      startActivity(new Intent(this, AccountDisplayActivity.class));
      return true;

    case R.id.menu_item_settings:
      startActivity(new Intent(this, SettingsActivity.class));
      return true;

    case R.id.menu_item_information:
      startActivity(new Intent(this, InformationActivity.class));
      return true;

    default:
      return super.onOptionsItemSelected(item);
    }
  }

  private void addListItem(String name, String number) {
    // TODO keep in memory current names and numbers
    // instead of reading them every time from the UI
    for (int i = 0; i < listSize; ++i) {
      View item = listLinear.getChildAt(i);
      TextView itemNumber = (TextView) item.findViewById(R.id.list_item_number);
      if (itemNumber.getText().toString().equals(number)) {
        Toast.makeText(ComposeActivity.this, R.string.receiver_toast,
            Toast.LENGTH_SHORT).show();
        return;
      }
    }

    View listItem = getLayoutInflater().inflate(R.layout.receiver_list_item,
        null);
    TextView listItemName = (TextView) listItem
        .findViewById(R.id.list_item_name);
    TextView listItemNumber = (TextView) listItem
        .findViewById(R.id.list_item_number);
    ImageButton listItemButton = (ImageButton) listItem
        .findViewById(R.id.list_item_button);

    listItemName.setText(name);
    listItemNumber.setText(number);
    listItemButton.setTag(listItem);
    listItemButton.setOnClickListener(new OnClickListener() {
      public void onClick(View view) {
        listLinear.removeView((View) view.getTag());
        if (--listSize == 0) {
          listLinear.setVisibility(View.GONE);
          replyLinear.setVisibility(View.VISIBLE);
          toggleButtons(receiverText.getText().length(), messageText.getText()
              .length());
        }
      }
    });

    listLinear.addView(listItem);
    if (++listSize == 1) {
      listLinear.setVisibility(View.VISIBLE);
      replyLinear.setVisibility(View.GONE);
      replyContent.setText("");
      replyDate.setText("");
      toggleButtons(receiverText.getText().length(), messageText.getText()
          .length());
    }
  }

  private void toggleButtons(int lReceiver, int lMessage) {
    if (lReceiver > 0 || listSize > 0 || lMessage > 0)
      clearButton.setEnabled(true);
    else
      clearButton.setEnabled(false);

    if ((lReceiver > 0 || listSize > 0) && account.calcRemaining(lMessage) > 0)
      sendButton.setEnabled(true);
    else
      sendButton.setEnabled(false);
  }

  private void clearFields() {
    String preference = preferences.getString("clear_message", "");
    if (preference.contains("R"))
      receiverText.setText("");
    if (preference.contains("M"))
      messageText.setText("");
  }
}