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

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.googlecode.awsms.R;

public class SettingsActivity extends PreferenceActivity {

  CheckBoxPreference enableNotifications;
  Preference notificationRingtone;
  Preference notificationVibration;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.layout.settings_activity);

    enableNotifications = 
        (CheckBoxPreference) findPreference("enable_notifications");
    enableNotifications.setOnPreferenceClickListener(
        new OnPreferenceClickListener() {
          public boolean onPreferenceClick(Preference preference) {
            toggleNotificationSettings();
            return false;
          }
        });

    notificationRingtone = 
        (Preference) findPreference("notification_ringtone");
    notificationVibration = 
        (Preference) findPreference("notification_vibration");
    toggleNotificationSettings();
  }

  private void toggleNotificationSettings() {
    if (enableNotifications.isChecked()) {
      notificationRingtone.setEnabled(true);
      notificationVibration.setEnabled(true);
    } else {
      notificationRingtone.setEnabled(false);
      notificationVibration.setEnabled(false);
    }
  }
}
