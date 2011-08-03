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

package com.googlecode.awsms.message;

import java.util.Date;

import com.googlecode.esms.message.ConversationManager;
import com.googlecode.esms.message.SMS;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import dalvik.system.PathClassLoader;

public class ConversationManagerAndroid extends ConversationManager {
  static final Uri SMS_INBOX = Uri.parse("content://sms/inbox");
  static final Uri SMS_SENT = Uri.parse("content://sms/sent");
  static final Uri SMS_DRAFT = Uri.parse("content://sms/draft");
  static final Uri SMS_OUTBOX = Uri.parse("content://sms/outbox");
  static final Uri SMS_FAILED = Uri.parse("content://sms/failed");
  
  static final String ADDRESS = "address";
  static final String BODY = "body";
  static final String DATE = "date";
  
  boolean telephony;
  Context context;
  
  public ConversationManagerAndroid(Context context) {
    this.context = context;
    
    try {
      // detect if SMS provider is installed
      // TODO test on different platforms (e.g. Archos)
      PathClassLoader cl = new PathClassLoader(
          "/system/app/TelephonyProvider.apk", context.getClassLoader());
      cl.loadClass("com.android.providers.telephony.SmsProvider");
      telephony = true;
    } catch (Exception e) {
      telephony = false;
      e.printStackTrace();
    }
  }
  
  @Override
  public SMS loadDraft(String receiver) {
    return load(SMS_DRAFT, receiver);
  }

  @Override
  public void saveDraft(SMS sms) {
    save(SMS_DRAFT, sms);
  }

  @Override
  public SMS loadInbox(String sender) {
    return load(SMS_INBOX, sender);
  }

  @Override
  public void saveInbox(SMS sms) {
    save(SMS_INBOX, sms);
  }

  @Override
  public SMS loadOutbox(String receiver) {
    return load(SMS_OUTBOX, receiver);
  }

  @Override
  public void saveOutbox(SMS sms) {
    delete(SMS_DRAFT, sms);
    delete(SMS_FAILED, sms);
    save(SMS_OUTBOX, sms);
  }

  @Override
  public SMS loadSent(String receiver) {
    return load(SMS_SENT, receiver);
  }
  
  @Override
  public void saveSent(SMS sms) {
    delete(SMS_OUTBOX, sms);
    save(SMS_SENT, sms);
  }

  @Override
  public SMS loadFailed(String receiver) {
    return load(SMS_FAILED, receiver);
  }

  @Override
  public void saveFailed(SMS sms) {
    delete(SMS_OUTBOX, sms);
    save(SMS_FAILED, sms);
  }
  
  private SMS load(Uri uri, String address) {
    int length = address.length();
    if (length > 10) address = address.substring(length-10, length);
    
    String projection[] = { ADDRESS, BODY, DATE }; 
    String selection = ADDRESS + " LIKE ?";
    String selectionArgs[] = { "%" + address };
    String sortOrder = DATE + " ASC";
    
    Cursor c = context.getContentResolver()
        .query(uri, projection, selection, selectionArgs, sortOrder);
    
    // return null if there are no messages
    if (c == null) return null;
    c.moveToLast();
    if (c.isAfterLast()) return null;
    
    String body = c.getString(c.getColumnIndex(BODY));
    long date = c.getLong(c.getColumnIndex(DATE));
    c.close();

    SMS sms = new SMS(body);
    sms.setDate(new Date(date).toLocaleString());
    sms.setReceiverNumber(new String[] { address });
    return sms;
  }

  private void save(Uri uri, SMS sms) {
    String body = sms.getMessage();
    for (String address : sms.getReceiverNumber()) {
      ContentValues values = new ContentValues();
      values.put(ADDRESS, address);
      values.put(BODY, body);
      context.getContentResolver().insert(uri, values);
    }
  }
  
  private int delete(Uri uri, SMS sms) {
    int rows = 0;
    String body = sms.getMessage();
    String where = ADDRESS + "=? AND " + BODY + "=?";
    for (String address : sms.getReceiverNumber()) {
      String selectionArgs[] = { address, body };
      rows += context.getContentResolver().delete(uri, where, selectionArgs);
    }
    return rows;
  }
}
