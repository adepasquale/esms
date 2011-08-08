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
import com.googlecode.esms.message.SMS;

public class AccountService extends Service {
  
  static final String TAG = "AccountService";

  SharedPreferences preferences;
  ConversationManager conversationManager;
  NotificationManager notificationManager;
  AudioManager audioManager;
  ProgressDialog progressDialog;
  
  // notification IDs
  static final int SENDING_NID = 1;
  static final int SUCCESSFUL_NID = 2;
  static final int CAPTCHA_NID = 3;
  static final int NETWORK_NID = 4;
  static final int ACCOUNT_NID = 5;
  static final int LIMIT_NID = 6;
  static final int RECEIVER_NID = 7;
  static final int MESSAGE_NID = 8;

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

  public void send(final Activity activity, final Account account, final SMS sms) {
    new AsyncTask<Void, Void, Result>() {
      
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
          AccountService.this.startForeground(
              SENDING_NID, createSendNotification(sms));
        }
      }

      @Override
      protected Result doInBackground(Void... params) {
        int attempts = 5;
        while (true) {
          Log.d(TAG, "call send() #" + attempts);
          Result result = account.send(sms);
          Log.d(TAG, "done send() " + result);
          AccountManager accountManager = new AccountManagerAndroid(activity);
          accountManager.delete(account);
          accountManager.insert(account);
          switch (result) {
          case PROVIDER_ERROR:
          case NETWORK_ERROR:
          case UNKNOWN_ERROR:
            if (--attempts == 0) return result;
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
            return result;
          }
        }
      }

      @Override
      protected void onPostExecute(Result result) {
        if (progress) {
          if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
          }
        }

        if (notifications) {
          AccountService.this.stopForeground(true);
        }
        
        switch (result) {
        case SUCCESSFUL:
          conversationManager.saveSent(sms);
          if (notifications)
            notificationManager.notify(
                SUCCESSFUL_NID, createSuccessfulNotification(sms));
          else // toast
            Toast.makeText(activity, R.string.sending_successful_toast, 
                Toast.LENGTH_LONG).show();
          break;
          
        case CAPTCHA_NEEDED:
        case CAPTCHA_ERROR:
          conversationManager.saveFailed(sms);
          showCaptchaDialog(activity, account, sms);
          if (notifications)
            notificationManager.notify(
                CAPTCHA_NID, createCaptchaNotification());
          break;
          
        case NETWORK_ERROR:
        case PROVIDER_ERROR:
        case UNKNOWN_ERROR:
          conversationManager.saveFailed(sms);
          showNetworkDialog(activity, account, sms);
          if (notifications)
            notificationManager.notify(
                NETWORK_NID, createNetworkNotification());
          break;
          
        case LOGIN_ERROR:
        case LOGOUT_ERROR:
        case SENDER_ERROR:
        case UNSUPPORTED_ERROR:
          conversationManager.saveFailed(sms);
          showAccountDialog(activity, account, sms);
          if (notifications)
            notificationManager.notify(
                ACCOUNT_NID, createAccountNotification());
          break;
          
        case LIMIT_ERROR:
          conversationManager.saveFailed(sms);
          showLimitDialog(activity, account, sms);
          if (notifications)
            notificationManager.notify(
                LIMIT_NID, createLimitNotification());
          break;
          
        case RECEIVER_ERROR:
          conversationManager.saveFailed(sms);
          showReceiverDialog(activity, account, sms);
          if (notifications)
            notificationManager.notify(
                LIMIT_NID, createReceiverNotification());
          break;
          
        case MESSAGE_ERROR:
          conversationManager.saveFailed(sms);
          showMessageDialog(activity, account, sms);
          if (notifications)
            notificationManager.notify(
                LIMIT_NID, createMessageNotification());
          break;
        }
      }

    }.execute();
  }

  private ProgressDialog showProgressDialog(Activity activity) {
    return ProgressDialog.show(activity, "",
        getString(R.string.sending_progress_dialog), true, false);
  }

  private Notification createSendNotification(SMS sms) {
    String receivers = "";
    String[] receiverName = sms.getReceiverName();
    String[] receiverNumber = sms.getReceiverNumber();
    for (int i = 0; i < receiverName.length; ++i) {
      if (receiverName[i] != null && !receiverName[i].equals("")) 
        receivers += receiverName[i];
      else receivers += receiverNumber[i];
      if (i != receiverName.length - 1)
        receivers += ", ";
    }
    
    String ticker = String.format(
        getString(R.string.sending_progress_notification), receivers);
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
    String receivers = "";
    String[] receiverName = sms.getReceiverName();
    String[] receiverNumber = sms.getReceiverNumber();
    for (int i = 0; i < receiverName.length; ++i) {
      if (receiverName[i] != null && !receiverName[i].equals("")) 
        receivers += receiverName[i];
      else receivers += receiverNumber[i];
      if (i != receiverName.length - 1)
        receivers += ", ";
    }
    
    String ticker = String.format(
        getString(R.string.sending_successful_notification), receivers);
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
  
  private void showCaptchaDialog(final Activity activity, 
      final Account account, final SMS sms) {
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
    byte[] captchaArray = sms.getCaptchaArray();
    Bitmap captchaBitmap = BitmapFactory.decodeByteArray(
        captchaArray, 0, captchaArray.length);
    BitmapDrawable captchaDrawable = new BitmapDrawable(captchaBitmap);
    captchaView.setBackgroundDrawable(captchaDrawable);
    
    builder.setPositiveButton(R.string.ok_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(CAPTCHA_NID);
            sms.setCaptcha(captchaText.getText().toString());
            send(activity, account, sms);
          }
        });

    builder.setNegativeButton(R.string.cancel_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(CAPTCHA_NID);
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
  
  private void showNetworkDialog(final Activity activity, 
      final Account account, final SMS sms) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

    builder.setTitle(R.string.network_error_dialog);
    builder.setMessage(R.string.network_error_message);

    builder.setPositiveButton(R.string.yes_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(NETWORK_NID);
            send(activity, account, sms);
          }
        });

    builder.setNegativeButton(R.string.no_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(NETWORK_NID);
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
  
  private void showAccountDialog(final Activity activity, 
      final Account account, final SMS sms) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

    builder.setTitle(R.string.account_error_dialog);
    builder.setMessage(R.string.account_error_message);

    builder.setPositiveButton(R.string.yes_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(ACCOUNT_NID);
            startActivity(new Intent(
                AccountService.this, AccountDisplayActivity.class));
          }
        });

    builder.setNegativeButton(R.string.no_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(ACCOUNT_NID);
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
  
  private void showLimitDialog(final Activity activity, 
      final Account account, final SMS sms) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

    builder.setTitle(R.string.limit_error_dialog);
    builder.setMessage(R.string.limit_error_message);

    builder.setPositiveButton(R.string.yes_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(LIMIT_NID);
            showResendDialog(activity, sms);
          }
        });

    builder.setNegativeButton(R.string.no_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(LIMIT_NID);
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
  
  private void showReceiverDialog(final Activity activity, 
      final Account account, final SMS sms) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

    builder.setTitle(R.string.receiver_error_dialog);
    builder.setMessage(R.string.receiver_error_message);

    builder.setPositiveButton(R.string.yes_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(RECEIVER_NID);
            showResendDialog(activity, sms);
          }
        });

    builder.setNegativeButton(R.string.no_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(RECEIVER_NID);
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
  
  private void showMessageDialog(final Activity activity, 
      final Account account, final SMS sms) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);

    builder.setTitle(R.string.message_error_dialog);
    builder.setMessage(R.string.message_error_message);

    builder.setPositiveButton(R.string.yes_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(MESSAGE_NID);
            showResendDialog(activity, sms);
          }
        });

    builder.setNegativeButton(R.string.no_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (preferences.getBoolean("enable_notifications", true))
              notificationManager.cancel(MESSAGE_NID);
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
  
  private void showResendDialog(final Activity activity, final SMS sms) {
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
}
