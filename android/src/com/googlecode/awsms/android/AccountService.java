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

import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.googlecode.awsms.R;
import com.googlecode.awsms.account.AccountConnectorAndroid;
import com.googlecode.awsms.account.AccountManagerAndroid;
import com.googlecode.awsms.message.ConversationManagerAndroid;
import com.googlecode.esms.account.Account;
import com.googlecode.esms.account.Account.Result;
import com.googlecode.esms.account.AccountConnector;
import com.googlecode.esms.account.AccountManager;
import com.googlecode.esms.message.ConversationManager;
import com.googlecode.esms.message.Receiver;
import com.googlecode.esms.message.SMS;

public class AccountService extends Service {
  
  static final String TAG = "AccountService";

  SharedPreferences preferences;
  ConversationManager conversationManager;
  NotificationManager notificationManager;
  AudioManager audioManager;
  ProgressDialog progressDialog;
  
  // notification IDs
  int currentNID;
  int sendingNID;
  int successfulNID;
  int captchaNID;
  int networkNID;
  int accountNID;
  int limitNID;
  int receiverNID;
  int messageNID;

  private final IBinder binder = new AccountServiceBinder();

  public class AccountServiceBinder extends Binder {
    public AccountService getService() {
      return AccountService.this;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override
  public void onCreate() {
    preferences = PreferenceManager.getDefaultSharedPreferences(this);
    conversationManager = new ConversationManagerAndroid(this);
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    currentNID = (int) new Date().getTime();
  }

//  @Override
//  public void onDestroy() {
//  }

  public void login(final Account account) {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        Log.d(TAG, "call login()");
        account.login();
        Log.d(TAG, "done login()");
        return null;
      }
    }.execute();
  }

  public void send(final ComposeActivity activity, 
      final Account account, final SMS sms) {
    new AsyncTask<Void, Void, List<Result>>() {
      
      // prevent errors when preferences are changed during send()
      boolean progress, notifications;

      @Override
      protected void onPreExecute() {
        conversationManager.saveOutbox(sms);
        
        progress = preferences.getBoolean("show_progress", true);
        if (progress) {
          progressDialog = showProgressDialog(activity);
        }

        notifications = preferences.getBoolean("enable_notifications", true);
        if (notifications) {
          sendingNID = currentNID++;
          AccountService.this.startForeground(
              sendingNID, createSendNotification(sms));
        }
      }

      @Override
      protected List<Result> doInBackground(Void... params) {
        int attempts = 5;
        while (true) {
          Log.d(TAG, "call send() #" + attempts);
          List<Result> results = account.send(sms);
          Log.d(TAG, "done send() " + results);
          AccountManager accountManager = new AccountManagerAndroid(activity);
          accountManager.delete(account);
          accountManager.insert(account);
          switch (results.get(0)) {
          case PROVIDER_ERROR:
          case NETWORK_ERROR:
          case UNKNOWN_ERROR:
            if (--attempts == 0) return results;
            AccountConnector connector = new AccountConnectorAndroid(activity);
            account.setAccountConnector(connector);
            try {
              Thread.sleep(2500);
            } catch (InterruptedException e) { 
              e.printStackTrace();
            }
            break;
//          case CAPTCHA_NEEDED:
//            byte[] captchaArray = sms.getCaptchaArray();
//            Bitmap captchaBitmap = BitmapFactory.decodeByteArray(
//                captchaArray, 0, captchaArray.length);
//            if (captchaBitmap == null) return Result.CAPTCHA_ERROR;
//            int w = captchaBitmap.getWidth();
//            int h = captchaBitmap.getHeight();
//            int[] pixels = new int[w*h];
//            captchaBitmap.getPixels(pixels, 0, w, 0, 0, w, h);
//            // (alpha << 24) | (red << 16) | (green << 8) | blue
//            // CaptchaDecoderAndroid.decode()
//            // break;
          default:
            return results;
          }
        }
      }

      @Override
      protected void onPostExecute(List<Result> results) {
        if (progress) {
          if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
          }
        }

        if (notifications) {
          AccountService.this.stopForeground(true);
        }
        
        // must handle partial failures
        int firstNotSuccessful = -1;
        for (int r = 0; r < results.size(); ++r) {
          if (results.get(r) != Result.SUCCESSFUL) {
            firstNotSuccessful = r;
            break;
          }
        }
        
        if (firstNotSuccessful == -1) { // all successful
          
          conversationManager.saveSent(sms);
          if (notifications) {
            successfulNID = currentNID++;
            notificationManager.notify(
                successfulNID, createSuccessfulNotification(sms));
          } else { // toast
            Toast.makeText(activity, R.string.sending_successful_toast, 
                Toast.LENGTH_LONG).show();
          }
          activity.clearFields();
          activity.refreshCounter();
          
        } else { // some not successful
          
          SMS successful = successfulSMS(sms, firstNotSuccessful);
          SMS unsuccessful = unsuccessfulSMS(sms, firstNotSuccessful);
          
          switch (results.get(firstNotSuccessful)) {
          case CAPTCHA_NEEDED:
          case CAPTCHA_ERROR:
            conversationManager.saveSent(successful);
            conversationManager.saveFailed(unsuccessful);
            showCaptchaDialog(activity, account, successful, unsuccessful);
            if (notifications) {
              captchaNID = currentNID++;
              notificationManager.notify(
                  captchaNID, createCaptchaNotification());
            }
            break;
            
          case NETWORK_ERROR:
          case PROVIDER_ERROR:
          case UNKNOWN_ERROR:
            conversationManager.saveSent(successful);
            conversationManager.saveFailed(unsuccessful);
            showNetworkDialog(activity, account, successful, unsuccessful);
            if (notifications) {
              networkNID = currentNID++;
              notificationManager.notify(
                  networkNID, createNetworkNotification());
            }
            break;
            
          case LOGIN_ERROR:
          case LOGOUT_ERROR:
          case SENDER_ERROR:
          case UNSUPPORTED_ERROR:
            conversationManager.saveSent(successful);
            conversationManager.saveFailed(unsuccessful);
            showAccountDialog(activity, account, successful, unsuccessful);
            if (notifications) {
              accountNID = currentNID++;
              notificationManager.notify(
                  accountNID, createAccountNotification());
            }
            break;
            
          case LIMIT_ERROR:
            conversationManager.saveSent(successful);
            conversationManager.saveFailed(unsuccessful);
            showLimitDialog(activity, account, successful, unsuccessful);
            if (notifications) {
              limitNID = currentNID++;
              notificationManager.notify(
                  limitNID, createLimitNotification());
            }
            AccountManager accountManager = 
                new AccountManagerAndroid(activity);
            // no more messages available, limit reached
            account.setCount(account.getLimit(), new Date());
            accountManager.delete(account);
            accountManager.insert(account);
            break;
            
          case RECEIVER_ERROR:
            conversationManager.saveSent(successful);
            conversationManager.saveFailed(unsuccessful);
            showReceiverDialog(activity, account, successful, unsuccessful);
            if (notifications) {
              receiverNID = currentNID++;
              notificationManager.notify(
                  receiverNID, createReceiverNotification());
            }
            break;
            
          case MESSAGE_ERROR:
            conversationManager.saveSent(successful);
            conversationManager.saveFailed(unsuccessful);
            showMessageDialog(activity, account, successful, unsuccessful);
            if (notifications) {
              messageNID = currentNID++;
              notificationManager.notify(
                  messageNID, createMessageNotification());
            }
            break;
          }
          
        }
      }

    }.execute();
  }

  private ProgressDialog showProgressDialog(Activity activity) {
    return ProgressDialog.show(activity, "",
        getString(R.string.sending_progress_dialog), true, false);
  }

  private Notification createSendNotification(SMS sms) {
    String ticker = String.format(
        getString(R.string.sending_progress_notification),
        receiverString(sms));
    Notification notification = new Notification(R.drawable.ic_stat_notify, 
        ticker, System.currentTimeMillis());
    Intent intent = new Intent(AccountService.this, ComposeActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
        Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    notification.setLatestEventInfo(AccountService.this,
        AccountService.this.getString(R.string.app_name), ticker,
        PendingIntent.getActivity(AccountService.this, 0, intent, 0));
    notification.sound = Uri.parse(preferences.getString(
        "notification_ringtone", "DEFAULT_RINGTONE_URI"));

    String vibrate = preferences.getString("notification_vibration", "S");
    if (vibrate.equals("A") || (vibrate.equals("S") && 
        audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)) {
      long[] pattern = { 0, 200, 100, 100 };
      notification.vibrate = pattern;
    }

    notification.flags |= Notification.FLAG_ONGOING_EVENT;
    notification.flags |= Notification.FLAG_NO_CLEAR;
    return notification;
  }
  
  private Notification createSuccessfulNotification(SMS sms) {
    String ticker = String.format(
        getString(R.string.sending_successful_notification), 
        receiverString(sms));
    Notification notification = new Notification(R.drawable.ic_stat_notify, 
        ticker, System.currentTimeMillis());
    Intent intent = new Intent(AccountService.this, ComposeActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
        Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    notification.setLatestEventInfo(AccountService.this,
        AccountService.this.getString(R.string.app_name), ticker,
        PendingIntent.getActivity(AccountService.this, 0, intent, 0));
    notification.sound = Uri.parse(preferences.getString(
        "notification_ringtone", "DEFAULT_RINGTONE_URI"));

    String vibrate = preferences.getString("notification_vibration", "S");
    if (vibrate.equals("A") || (vibrate.equals("S") && 
        audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)) {
      long[] pattern = { 0, 200, 100, 100 };
      notification.vibrate = pattern;
    }

    notification.flags |= Notification.FLAG_AUTO_CANCEL;
    return notification;
  }
  
  // FIXME handle partial failures
  private void showCaptchaDialog(final ComposeActivity activity, 
      final Account account, final SMS successful, final SMS unsuccessful) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

    LayoutInflater inflater = (LayoutInflater) this
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    LinearLayout decodeLinear = (LinearLayout) inflater.inflate(
        R.layout.captcha_decode_dialog, null);
    final CaptchaView captchaView = (CaptchaView) decodeLinear
        .findViewById(R.id.captcha_image);
    final EditText captchaText = (EditText) decodeLinear
        .findViewById(R.id.captcha_text);
    
    builder.setTitle(R.string.captcha_decode_dialog);
    builder.setView(decodeLinear);
    byte[] captchaArray = unsuccessful.getCaptchaArray();
    Bitmap captchaBitmap = BitmapFactory.decodeByteArray(
        captchaArray, 0, captchaArray.length);
    BitmapDrawable captchaDrawable = new BitmapDrawable(captchaBitmap);
    captchaView.setBackgroundDrawable(captchaDrawable);
    
    builder.setOnKeyListener(new OnKeyListener() {
      @Override
      public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
          if (preferences.getBoolean("enable_notifications", true))
            notificationManager.cancel(captchaNID);
          unsuccessful.setCaptchaText(captchaText.getText().toString());
          send(activity, account, unsuccessful);
          dialog.dismiss();
          return true;
        }
        return false;
      }
    });
    
    builder.setPositiveButton(R.string.ok_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(captchaNID);
            unsuccessful.setCaptchaText(captchaText.getText().toString());
            send(activity, account, unsuccessful);
          }
        });

    builder.setNegativeButton(R.string.cancel_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(captchaNID);
            Toast.makeText(activity, R.string.sending_canceled_toast, 
                Toast.LENGTH_LONG).show();
          }
        });
    
    builder.setOnCancelListener(new OnCancelListener() {
          public void onCancel(DialogInterface dialog) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(captchaNID);
            Toast.makeText(activity, R.string.sending_canceled_toast, 
                Toast.LENGTH_LONG).show();
          }
        });

    builder.show();
    captchaText.requestFocus();
  }
  
  private Notification createCaptchaNotification() {
    String ticker = getString(R.string.captcha_decode_notification);
    Notification notification = new Notification(R.drawable.ic_stat_notify, 
        ticker, System.currentTimeMillis());
    Intent intent = new Intent(AccountService.this, ComposeActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
        Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    notification.setLatestEventInfo(AccountService.this,
        AccountService.this.getString(R.string.app_name), ticker,
        PendingIntent.getActivity(AccountService.this, 0, intent, 0));
    notification.sound = Uri.parse(preferences.getString(
        "notification_ringtone", "DEFAULT_RINGTONE_URI"));

    String vibrate = preferences.getString("notification_vibration", "S");
    if (vibrate.equals("A") || (vibrate.equals("S") && 
        audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)) {
      long[] pattern = { 0, 200, 100, 100 };
      notification.vibrate = pattern;
    }

    notification.flags |= Notification.FLAG_AUTO_CANCEL;
    return notification;
  }
  
  // FIXME handle partial failures
  private void showNetworkDialog(final ComposeActivity activity, 
      final Account account, final SMS successful, final SMS unsuccessful) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

    builder.setTitle(R.string.network_error_dialog);
    builder.setMessage(R.string.network_error_message);

    builder.setPositiveButton(R.string.yes_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(networkNID);
            send(activity, account, unsuccessful);
          }
        });

    builder.setNegativeButton(R.string.no_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(networkNID);
            Toast.makeText(activity, R.string.sending_canceled_toast, 
                Toast.LENGTH_LONG).show();
          }
        });

    builder.show();
  }
  
  private Notification createNetworkNotification() {
    String ticker = getString(R.string.network_error_notification);
    Notification notification = new Notification(R.drawable.ic_stat_notify, 
        ticker, System.currentTimeMillis());
    Intent intent = new Intent(AccountService.this, ComposeActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
        Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    notification.setLatestEventInfo(AccountService.this,
        AccountService.this.getString(R.string.app_name), ticker,
        PendingIntent.getActivity(AccountService.this, 0, intent, 0));
    notification.sound = Uri.parse(preferences.getString(
        "notification_ringtone", "DEFAULT_RINGTONE_URI"));

    String vibrate = preferences.getString("notification_vibration", "S");
    if (vibrate.equals("A") || (vibrate.equals("S") && 
        audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)) {
      long[] pattern = { 0, 200, 100, 100 };
      notification.vibrate = pattern;
    }

    notification.flags |= Notification.FLAG_AUTO_CANCEL;
    return notification;
  }
  
  // FIXME handle partial failures
  private void showAccountDialog(final Activity activity, 
      final Account account, final SMS successful, final SMS unsuccessful) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

    builder.setTitle(R.string.account_error_dialog);
    builder.setMessage(R.string.account_error_message);

    builder.setPositiveButton(R.string.yes_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(accountNID);
            startActivity(new Intent(
                AccountService.this, AccountDisplayActivity.class));
          }
        });

    builder.setNegativeButton(R.string.no_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(accountNID);
            Toast.makeText(activity, R.string.sending_canceled_toast, 
                Toast.LENGTH_LONG).show();
          }
        });

    builder.show();
  }
  
  private Notification createAccountNotification() {
    String ticker = getString(R.string.account_error_notification);
    Notification notification = new Notification(R.drawable.ic_stat_notify, 
        ticker, System.currentTimeMillis());
    Intent intent = new Intent(AccountService.this, ComposeActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
        Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    notification.setLatestEventInfo(AccountService.this,
        AccountService.this.getString(R.string.app_name), ticker,
        PendingIntent.getActivity(AccountService.this, 0, intent, 0));
    notification.sound = Uri.parse(preferences.getString(
        "notification_ringtone", "DEFAULT_RINGTONE_URI"));

    String vibrate = preferences.getString("notification_vibration", "S");
    if (vibrate.equals("A") || (vibrate.equals("S") && 
        audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)) {
      long[] pattern = { 0, 200, 100, 100 };
      notification.vibrate = pattern;
    }

    notification.flags |= Notification.FLAG_AUTO_CANCEL;
    return notification;
  }
  
  // FIXME handle partial failures
  private void showLimitDialog(final ComposeActivity activity, 
      final Account account, final SMS successful, final SMS unsuccessful) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

    builder.setTitle(R.string.limit_error_dialog);
    builder.setMessage(R.string.limit_error_message);

    builder.setPositiveButton(R.string.yes_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(limitNID);
            showResendDialog(activity, unsuccessful);
          }
        });

    builder.setNegativeButton(R.string.no_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(limitNID);
            Toast.makeText(activity, R.string.sending_canceled_toast, 
                Toast.LENGTH_LONG).show();
          }
        });

    builder.show();
  }
  
  private Notification createLimitNotification() {
    String ticker = getString(R.string.limit_error_notification);
    Notification notification = new Notification(R.drawable.ic_stat_notify, 
        ticker, System.currentTimeMillis());
    Intent intent = new Intent(AccountService.this, ComposeActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
        Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    notification.setLatestEventInfo(AccountService.this,
        AccountService.this.getString(R.string.app_name), ticker,
        PendingIntent.getActivity(AccountService.this, 0, intent, 0));
    notification.sound = Uri.parse(preferences.getString(
        "notification_ringtone", "DEFAULT_RINGTONE_URI"));

    String vibrate = preferences.getString("notification_vibration", "S");
    if (vibrate.equals("A") || (vibrate.equals("S") && 
        audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)) {
      long[] pattern = { 0, 200, 100, 100 };
      notification.vibrate = pattern;
    }

    notification.flags |= Notification.FLAG_AUTO_CANCEL;
    return notification;
  }
  
  // FIXME handle partial failures
  private void showReceiverDialog(final ComposeActivity activity, 
      final Account account, final SMS successful, final SMS unsuccessful) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

    builder.setTitle(R.string.receiver_error_dialog);
    builder.setMessage(R.string.receiver_error_message);

    builder.setPositiveButton(R.string.yes_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(receiverNID);
            showResendDialog(activity, unsuccessful);
          }
        });

    builder.setNegativeButton(R.string.no_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(receiverNID);
            Toast.makeText(activity, R.string.sending_canceled_toast, 
                Toast.LENGTH_LONG).show();
          }
        });

    builder.show();
  }
  
  private Notification createReceiverNotification() {
    String ticker = getString(R.string.receiver_error_notification);
    Notification notification = new Notification(R.drawable.ic_stat_notify, 
        ticker, System.currentTimeMillis());
    Intent intent = new Intent(AccountService.this, ComposeActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
        Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    notification.setLatestEventInfo(AccountService.this,
        AccountService.this.getString(R.string.app_name), ticker,
        PendingIntent.getActivity(AccountService.this, 0, intent, 0));
    notification.sound = Uri.parse(preferences.getString(
        "notification_ringtone", "DEFAULT_RINGTONE_URI"));

    String vibrate = preferences.getString("notification_vibration", "S");
    if (vibrate.equals("A") || (vibrate.equals("S") && 
        audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)) {
      long[] pattern = { 0, 200, 100, 100 };
      notification.vibrate = pattern;
    }

    notification.flags |= Notification.FLAG_AUTO_CANCEL;
    return notification;
  }
  
  // FIXME handle partial failures
  private void showMessageDialog(final ComposeActivity activity, 
      final Account account, final SMS successful, final SMS unsuccessful) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

    builder.setTitle(R.string.message_error_dialog);
    builder.setMessage(R.string.message_error_message);

    builder.setPositiveButton(R.string.yes_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(messageNID);
            showResendDialog(activity, unsuccessful);
          }
        });

    builder.setNegativeButton(R.string.no_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(messageNID);
            Toast.makeText(activity, R.string.sending_canceled_toast, 
                Toast.LENGTH_LONG).show();
          }
        });

    builder.show();
  }
  
  private Notification createMessageNotification() {
    String ticker = getString(R.string.message_error_notification);
    Notification notification = new Notification(R.drawable.ic_stat_notify, 
        ticker, System.currentTimeMillis());
    Intent intent = new Intent(AccountService.this, ComposeActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
        Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    notification.setLatestEventInfo(AccountService.this,
        AccountService.this.getString(R.string.app_name), ticker,
        PendingIntent.getActivity(AccountService.this, 0, intent, 0));
    notification.sound = Uri.parse(preferences.getString(
        "notification_ringtone", "DEFAULT_RINGTONE_URI"));

    String vibrate = preferences.getString("notification_vibration", "S");
    if (vibrate.equals("A") || (vibrate.equals("S") && 
        audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)) {
      long[] pattern = { 0, 200, 100, 100 };
      notification.vibrate = pattern;
    }

    notification.flags |= Notification.FLAG_AUTO_CANCEL;
    return notification;
  }
  
  private void showResendDialog(final ComposeActivity activity, final SMS sms) {
    AccountManager accountManager = new AccountManagerAndroid(this);
    final List<Account> accounts = accountManager.getAccounts();
    final CharSequence[] accountLabels = new CharSequence[accounts.size()];
    for (int a = 0; a < accounts.size(); ++a) {
      String label = accounts.get(a).getLabel();
      if (label == null || label.equals(""))
        label = getString(R.string.no_label_text);
      accountLabels[a] = label;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    builder.setTitle(R.string.account_prompt_dialog);
    builder.setSingleChoiceItems(accountLabels, -1, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int item) {
        dialog.dismiss();
        send(activity, accounts.get(item), sms);
      }
    });
    
    builder.setCancelable(true);
    builder.setOnCancelListener(new OnCancelListener() {
      public void onCancel(DialogInterface dialog) {
        Toast.makeText(activity, R.string.sending_canceled_toast, 
            Toast.LENGTH_LONG).show();
      }
    });

    builder.show();
  }
  
  private String receiverString(SMS sms) {
    String receiverString = "";
    List<Receiver> receivers = sms.getReceivers();
    switch (receivers.size()) {
    case 2:
      Receiver r1 = receivers.get(1);
      receiverString = " "+getString(R.string.and_connector)+" ";
      if (r1.getName() != null && !r1.getName().equals("")) 
        receiverString += r1.getName();
      else receiverString += r1.getNumber();
    case 1:
      Receiver r0 = receivers.get(0);
      if (r0.getName() != null && !r0.getName().equals(""))
        receiverString = r0.getName() + receiverString;
      else receiverString = r0.getNumber() + receiverString;
      break;
      
    default:
      Receiver r = receivers.get(0);
      if (r.getName() != null && !r.getName().equals("")) 
        receiverString += r.getName();
      else receiverString += r.getNumber();
      receiverString += " "+getString(R.string.and_connector)+" "+
          getString(R.string.other_connector)+" "+(receivers.size()-1);      
    }
    
    return receiverString;
  }

  private SMS successfulSMS(SMS sms, int where) {
    SMS successful = sms.clone();
    List<Receiver> receivers = sms.getReceivers();
    for (int r = where; r < receivers.size(); ++r)
      successful.removeReceiver(receivers.get(r));
    return successful;
  }
  
  private SMS unsuccessfulSMS(SMS sms, int where) {
    SMS unsuccessful = sms.clone();
    List<Receiver> receivers = sms.getReceivers();
    for (int r = 0; r < where && r < receivers.size(); ++r)
      unsuccessful.removeReceiver(receivers.get(r));
    return unsuccessful;
  }
}
