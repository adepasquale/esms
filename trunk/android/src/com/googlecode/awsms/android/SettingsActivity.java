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
