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

package com.googlecode.ermete.android.service;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.googlecode.ermete.R;
import com.googlecode.ermete.account.Account;
import com.googlecode.ermete.android.activity.ComposeActivity;
import com.googlecode.ermete.sms.SMS;

public class AccountService extends Service {

  static final String TAG = "AccountService";

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
    Log.d(TAG, "onBind()");
    return binder;
  }

  @Override
  public void onCreate() {
    Log.d(TAG, "onCreate()");
    preferences = PreferenceManager.getDefaultSharedPreferences(this);
    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy()");
  }

  public void login(final Account account) {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        Log.d(TAG, "this.login()");
        account.login();
        return null;
      }
    }.execute();
  }

  public void send(final Activity activity, final Account account, final SMS sms) {
    new AsyncTask<Void, Void, SMS>() {
      
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
          AccountService.this.startForeground(12345,
              createNotification(sms.getReceiverName()));
        }
      }

      @Override
      protected SMS doInBackground(Void... params) {
        Log.d(TAG, "this.send()");

        String receiverName[] = null;
        String receiverNumber[] = null;
        receiverName = sms.getReceiverName();
        receiverNumber = sms.getReceiverNumber();

        Log.d(TAG, sms.getMessage());
        for (int i = 0; i < receiverNumber.length; ++i) {
          Log.d(TAG, receiverName[i] + " " + receiverNumber[i]);
        }

        account.send(sms);
        
        return null;
      }

      @Override
      protected void onPostExecute(SMS result) {
        if (progress) {
          if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
          }
        }

        if (notifications) {
          AccountService.this.stopForeground(true);
        }
      }

    }.execute();
  }

  private ProgressDialog showProgressDialog(Activity activity) {
    return ProgressDialog.show(activity, "",
        getString(R.string.sending_progress_dialog), true, false);
  }

  private Notification createNotification(String[] receivers) {
    String ticker = getString(R.string.sending_notification_1) + " "
        + receivers[0] + " " + getString(R.string.sending_notification_2);
    Notification notification = new Notification(R.drawable.ic_stat_notify, ticker,
        System.currentTimeMillis());
    Intent intent = new Intent(AccountService.this, ComposeActivity.class);
    intent.setAction(Intent.ACTION_MAIN);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
        | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    notification.setLatestEventInfo(AccountService.this,
        AccountService.this.getString(R.string.app_name), ticker,
        PendingIntent.getActivity(AccountService.this, 0, intent, 0));
    notification.sound = Uri.parse(preferences.getString(
        "notification_ringtone", "DEFAULT_RINGTONE_URI"));

    String vibrate = preferences.getString("notification_vibration", "S");
    if (vibrate.equals("A")
        || (vibrate.equals("S") && audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)) {
      long[] pattern = { 0, 300, 100, 100 };
      notification.vibrate = pattern;
    }

    notification.flags |= Notification.FLAG_ONGOING_EVENT;
    notification.flags |= Notification.FLAG_NO_CLEAR;
    return notification;
  }
}
