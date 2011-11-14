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

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.text.Annotation;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
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

import com.googlecode.awsms.R;
import com.googlecode.awsms.account.AccountManagerAndroid;
import com.googlecode.awsms.android.AccountService.AccountServiceBinder;
import com.googlecode.awsms.message.ConversationManagerAndroid;
import com.googlecode.esms.account.Account;
import com.googlecode.esms.account.AccountManager;
import com.googlecode.esms.message.ConversationManager;
import com.googlecode.esms.message.Receiver;
import com.googlecode.esms.message.SMS;

public class ComposeActivity extends Activity {
  
  ConversationManager conversationManager;
  AccountManager accountManager;
  Account account;

  SharedPreferences preferences;

  ServiceConnection serviceConnection;
  AccountService accountService;
  boolean serviceBound;
  
  BroadcastReceiver broadcastReceiver;
  IntentFilter intentFilter;
  static final String ACTION_SMS_RECEIVED = 
      "android.provider.Telephony.SMS_RECEIVED";

  ImageView counterImage;
  AnimationDrawable counterAnimation;
  TextView counterText;

  Spinner senderSpinner;
  ImageButton clearButton;

  AutoCompleteTextView receiverText;
  String receiverIncomplete;
  boolean hasAutocompleted;
  ImageButton plusButton;

  LinearLayout listLinear;
  int listSize;
  static final int MAX_LIST_SIZE = 3;

  LinearLayout replyLinear;
  TextView replyContent;
  TextView replyDate;

  EditText messageText;
  TextWatcher messageWatcher;
  Button sendButton;
  TextView lengthText;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.compose_activity);

    preferences = PreferenceManager.getDefaultSharedPreferences(this);
    conversationManager = new ConversationManagerAndroid(this);
    accountManager = new AccountManagerAndroid(ComposeActivity.this);

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

    intentFilter = new IntentFilter();
    intentFilter.addAction(ACTION_SMS_RECEIVED);
    broadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
          Object[] pdus = (Object[]) bundle.get("pdus");
          for (Object pdu : pdus) {
            SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu);
            if (listSize == 0) {
              String number = receiverText.getText().toString();
              Spannable s = receiverText.getText();
              Annotation[] annotations = 
                  s.getSpans(0, s.length(), Annotation.class);
              for (Annotation a : annotations)
                if (a.getKey().equals("number"))
                  number = a.getValue();
              
              String address = message.getOriginatingAddress();
              int addressLength = address.length();
              if (addressLength > 10) 
                address = address.substring(addressLength-10, addressLength);
              int numberLength = number.length();
              if (numberLength > 10) 
                number = number.substring(numberLength-10, numberLength);
              
              if (number.equals(address)) {
                replyContent.setText(message.getMessageBody());
                long date = message.getTimestampMillis();
                replyDate.setText(new Date(date).toLocaleString());
              }
            }
          }
        }
      }
    };
    registerReceiver(broadcastReceiver, intentFilter);
    
    clearButton = (ImageButton) findViewById(R.id.clear_button);
    clearButton.setEnabled(false);
    clearButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        clearButton.setEnabled(false);
        receiverText.setText("");
        receiverText.requestFocus();
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
      public void onItemClick(AdapterView<?> parent, 
          View view, int position, long id) {
        TextView receiverName = (TextView) view
            .findViewById(R.id.receiver_name);
        TextView receiverNumber = (TextView) view
            .findViewById(R.id.receiver_number);

        hasAutocompleted = true;

        if (listSize == 0) {
          SMS reply = 
            conversationManager.loadInbox(receiverNumber.getText().toString());
          if (reply != null) {
            replyContent.setText(reply.getMessage());
            replyDate.setText(reply.getDate());
          }
          messageText.requestFocus();
        } else if (listSize < MAX_LIST_SIZE) {
          String name = receiverName.getText().toString();
          String number = receiverNumber.getText().toString();
          addListItem(name, number);
          receiverText.setText("");
          receiverText.requestFocus();
          String t = String.format(
              getString(R.string.positive_receiver_toast), 
              name + " (" + number + ")");
          Toast.makeText(ComposeActivity.this, t, Toast.LENGTH_SHORT).show();
        } else {
          receiverText.setText("");
          receiverText.requestFocus();
          String t = getString(R.string.maximum_receivers_toast);
          Toast.makeText(ComposeActivity.this, t, Toast.LENGTH_SHORT).show();
        }

        toggleButtons(receiverText.getText().length(), 
            messageText.getText().length());
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
        if (number.equals("")) return;
        
        if (listSize >= MAX_LIST_SIZE) {
          String t = getString(R.string.maximum_receivers_toast);
          Toast.makeText(ComposeActivity.this, t, Toast.LENGTH_SHORT).show();
          return;
        }

        addListItem(name, number);
        if (listSize > 1) {
          String t = String.format(
              getString(R.string.positive_receiver_toast), number);
          Toast.makeText(ComposeActivity.this, t, Toast.LENGTH_SHORT).show();
        }
        receiverText.setText("");
        receiverText.requestFocus();
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
    sendButton = (Button) findViewById(R.id.send);
    lengthText = (TextView) findViewById(R.id.length);
    
    // check if application was started with an intent
    Intent intent = getIntent();
    if (savedInstanceState == null && 
        intent != null && intent.getAction() != null) {
      if (intent.getAction().equals(Intent.ACTION_SEND)) {
        String message = intent.getStringExtra(Intent.EXTRA_TEXT);
        messageText.setText(message);
        clearButton.setEnabled(true);
        receiverText.requestFocus();
      }

      if (intent.getAction().equals(Intent.ACTION_SENDTO)) {
        String receiver = URLDecoder.decode(
            intent.getDataString()).replaceAll("[^0-9\\+]*", "");
        ReceiverAdapter receiverAdapter = new ReceiverAdapter(this);
        Cursor cursor = receiverAdapter.runQueryOnBackgroundThread(receiver);
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
          receiverText.setText(receiverAdapter.convertToString(cursor));
        } else {
          receiverText.setText(receiver);
        }
        messageText.requestFocus();
        clearButton.setEnabled(true);
        SMS reply = conversationManager.loadInbox(receiver);
        if (reply != null) {
          replyContent.setText(reply.getMessage());
          replyDate.setText(reply.getDate());
        }
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume(); 
    
    if (accountManager.getAccounts().size() == 0) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(R.string.app_name);
      builder.setMessage(R.string.welcome_message);
      builder.setPositiveButton(R.string.create_button, 
          new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
          startActivity(new Intent(
              ComposeActivity.this,
              AccountCreateActivity.class));
        }
      });
      builder.setCancelable(false);
      builder.show();
    }
    
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
    senderAdapter.setDropDownViewResource(
        android.R.layout.simple_spinner_dropdown_item);
    senderSpinner = (Spinner) findViewById(R.id.sender_spinner);
    senderSpinner.setAdapter(senderAdapter);
    senderSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onNothingSelected(AdapterView<?> parent) { }
      public void onItemSelected(
          AdapterView<?> parent, View view, int position, long id) {
        List<Account> accounts = accountManager.getAccounts();
        account = accounts.get(position);
        refreshCounter();
        if (accountService != null)
          accountService.login(account);
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
    
    messageWatcher = new TextWatcher() {
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
    };
    
    messageText.addTextChangedListener(messageWatcher);

    sendButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        List<Receiver> receivers = new LinkedList<Receiver>();
        if (listSize == 0) {
          String name = null;
          String number = receiverText.getText().toString();
          Spannable s = receiverText.getText();
          Annotation[] annotations = s.getSpans(0, s.length(), Annotation.class);
          for (Annotation a : annotations) {
            if (a.getKey().equals("name"))
              name = a.getValue();
            if (a.getKey().equals("number"))
              number = a.getValue();
          }
          receivers.add(new Receiver(name, number));
        } else {
          for (int i = 0; i < listSize; ++i) {
            View listItem = listLinear.getChildAt(i);
            TextView listItemName = (TextView) listItem
                .findViewById(R.id.list_item_name);
            TextView listItemNumber = (TextView) listItem
                .findViewById(R.id.list_item_number);
            receivers.add(new Receiver(
                listItemName.getText().toString(),
                listItemNumber.getText().toString()));
          }
        }
        
        for (Receiver receiver : receivers) {
          receiver.setNumber(receiver.getNumber().replaceAll("[^0-9\\+]*", ""));
          int length = receiver.getNumber().length();
          if (length < 9 || length > 13) {
            Toast.makeText(ComposeActivity.this,
                R.string.invalid_receiver_toast, Toast.LENGTH_SHORT).show();
            return;
          }
        }
        
        // TODO ask confirmation if account.getCount() == account.getLimit()
        
        SMS sms = new SMS(messageText.getText().toString(), receivers);
        if (accountService != null)
          accountService.send(ComposeActivity.this, account, sms);
        
        if (preferences.getBoolean("background_send", false)) {
          Toast.makeText(ComposeActivity.this, R.string.sending_toast,
              Toast.LENGTH_LONG).show();
          
          counterImage.setImageResource(R.drawable.sending_progress);
          counterAnimation = (AnimationDrawable) counterImage.getDrawable();
          counterAnimation.start();

          // disable the button and re-enable it after some time
          sendButton.setEnabled(false);
          new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
              try {
                Thread.sleep(3500); // Toast.LENGTH_LONG
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
              return null;
            }

            protected void onPostExecute(Void result) {
              sendButton.setEnabled(true);
            };
          }.execute();
        }
      }
    });
  }
  
  @Override
  public void onPause() {
    senderSpinner.setOnItemSelectedListener(null);
    messageText.removeTextChangedListener(messageWatcher);
    sendButton.setOnClickListener(null);
    
    // TODO save app status (account, single/multiple receiver, message text)
    super.onPause();
  }
  
  @Override
  public void onDestroy() {
    if (serviceBound) {
      unbindService(serviceConnection);
      serviceBound = false;
    }
    
    unregisterReceiver(broadcastReceiver);
    super.onDestroy();
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
      showInformation();
      return true;
      
    case R.id.menu_item_reporting:
      // TODO use IssueTrackerAPI with custom input form
      Intent reportingIntent = new Intent(Intent.ACTION_VIEW);
      final String REPORTING_URL = 
          "http://code.google.com/p/esms/issues/list";
      reportingIntent.setData(Uri.parse(REPORTING_URL));
      startActivity(reportingIntent);
      return true;

    default:
      return super.onOptionsItemSelected(item);
    }
  }
  
  public void clearFields() {
    String preference = preferences.getString("clear_message", "M");
    if (preference.contains("R")) receiverText.setText("");
    if (preference.contains("M")) messageText.setText("");
  }

  public void refreshCounter() {
    int limit = account.getLimit();
    int remaining = limit - account.getCount();
    if (remaining < 0) remaining = 0;
    if (remaining > limit) remaining = limit;
    counterText.setText(Integer.toString(remaining));
    int percentage = (int) 10.0 * remaining / limit;
    switch (percentage) {
    case  0: counterImage.setImageResource(R.drawable.ic_counter_0);  break;
    case  1: counterImage.setImageResource(R.drawable.ic_counter_1);  break;
    case  2: counterImage.setImageResource(R.drawable.ic_counter_2);  break;
    case  3: counterImage.setImageResource(R.drawable.ic_counter_3);  break;
    case  4: counterImage.setImageResource(R.drawable.ic_counter_4);  break;
    case  5: counterImage.setImageResource(R.drawable.ic_counter_5);  break;
    case  6: counterImage.setImageResource(R.drawable.ic_counter_6);  break;
    case  7: counterImage.setImageResource(R.drawable.ic_counter_7);  break;
    case  8: counterImage.setImageResource(R.drawable.ic_counter_8);  break;
    case  9: counterImage.setImageResource(R.drawable.ic_counter_9);  break;
    case 10: counterImage.setImageResource(R.drawable.ic_counter_10); break;
    }
  }
  
  private void addListItem(String name, String number) {
    for (int i = 0; i < listSize; ++i) {
      View item = listLinear.getChildAt(i);
      TextView itemNumber = (TextView) item.findViewById(R.id.list_item_number);
      if (itemNumber.getText().toString().equals(number)) {
        Toast.makeText(ComposeActivity.this, R.string.negative_receiver_toast,
            Toast.LENGTH_SHORT).show();
        return;
      }
    }

    View listItem = getLayoutInflater().inflate(
        R.layout.receiver_list_item, null);
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
          toggleButtons(receiverText.getText().length(), 
              messageText.getText().length());
        }
      }
    });

    listLinear.addView(listItem);
    if (++listSize == 1) {
      listLinear.setVisibility(View.VISIBLE);
      replyLinear.setVisibility(View.GONE);
      replyContent.setText("");
      replyDate.setText("");
      toggleButtons(receiverText.getText().length(), 
          messageText.getText().length());
    }
  }

  private void toggleButtons(int lReceiver, int lMessage) {
    if (lReceiver > 0 || listSize > 0 || lMessage > 0)
      clearButton.setEnabled(true);
    else
      clearButton.setEnabled(false);

    if ((lReceiver > 0 || listSize > 0) && account.calcFragments(lMessage) > 0)
      sendButton.setEnabled(true);
    else
      sendButton.setEnabled(false);
  }
  
  private void showInformation() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);

    LayoutInflater inflater = (LayoutInflater)
        this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    LinearLayout informationLinear = (LinearLayout) 
        inflater.inflate(R.layout.information_dialog, null);
    TextView versionText = (TextView) 
        informationLinear.findViewById(R.id.app_version);
    
    builder.setView(informationLinear);
    builder.setNeutralButton(R.string.close_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
          }
        });
    
    try {
      PackageInfo packageInfo = 
          getPackageManager().getPackageInfo(getPackageName(), 0);
      versionText.setText(String.format(
          getString(R.string.app_version),
          packageInfo.versionName));
    } catch (NameNotFoundException e) {
      e.printStackTrace();
    }

    builder.show();
  }
}