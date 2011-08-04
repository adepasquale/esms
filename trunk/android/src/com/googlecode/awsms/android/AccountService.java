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
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.googlecode.awsms.account.AccountConnectorAndroid;
import com.googlecode.awsms.account.AccountManagerAndroid;
import com.googlecode.awsms.R;
import com.googlecode.esms.account.Account;
import com.googlecode.esms.account.AccountConnector;
import com.googlecode.esms.account.AccountManager;
import com.googlecode.esms.account.Account.Result;
import com.googlecode.esms.message.SMS;

public class AccountService extends Service {

  SharedPreferences preferences;
  AudioManager audioManager;
  ProgressDialog progressDialog;

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
    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
  }

//  @Override
//  public void onDestroy() {
//  }

  public void login(final Account account) {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        System.out.println("call login()");
        account.login();
        System.out.println("done login()");
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
        progress = preferences.getBoolean("show_progress", false);
        if (progress) {
          progressDialog = showProgressDialog(activity);
        }

        notifications = preferences.getBoolean("enable_notifications", true);
        if (notifications) {
          AccountService.this.startForeground(
              12345, createSendNotification(sms));
        }
      }

      @Override
      protected Result doInBackground(Void... params) {
        int attempts = 3;
        while (true) {
          System.out.println("call send() #" + attempts);
          Result result = account.send(sms);
          System.out.println("done send() " + result);
          AccountManager accountManager = new AccountManagerAndroid(activity);
          accountManager.delete(account);
          accountManager.insert(account);
          switch (result) {
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
          case CAPTCHA_NEEDED:
            byte[] captchaArray = sms.getCaptchaArray();
            Bitmap captchaBitmap = BitmapFactory.decodeByteArray(
                captchaArray, 0, captchaArray.length);
            if (captchaBitmap == null) return Result.CAPTCHA_ERROR;
            int w = captchaBitmap.getWidth();
            int h = captchaBitmap.getHeight();
            int[] pixels = new int[w*h];
            captchaBitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            // (alpha << 24) | (red << 16) | (green << 8) | blue
//            break;
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
          if (notifications) {
            
          } else { // toast
            
          }
          break;
        case CAPTCHA_NEEDED:
        case CAPTCHA_ERROR:
          /* decode manually */
          break;
        case NETWORK_ERROR:
        case UNKNOWN_ERROR:
          /* network is down? */
          break;
        case LOGIN_ERROR:
        case LOGOUT_ERROR:
        case SENDER_ERROR:
        case UNSUPPORTED_ERROR:
          /* account configuration */
          break;
        case RECEIVER_ERROR:
        case MESSAGE_ERROR:
        case LIMIT_ERROR:
        case PROVIDER_ERROR:
          /* send with other account or with default app */
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
        getString(R.string.sending_notification), receivers);
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
}
