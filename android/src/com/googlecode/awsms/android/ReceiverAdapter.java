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

import java.io.InputStream;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.text.Annotation;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.View;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.googlecode.awsms.R;

public class ReceiverAdapter extends ResourceCursorAdapter implements
    Filterable {

  final static String TAG = "ReceiverAdapter";

  Context context;
  SharedPreferences preferences;

  public ReceiverAdapter(Context context) {
    super(context, R.layout.receiver_adapter, null, false);
    this.context = context;
    preferences = PreferenceManager.getDefaultSharedPreferences(context);
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    ImageView image = (ImageView) view.findViewById(R.id.receiver_image);

    if (preferences.getBoolean("show_pictures", true)) {
      InputStream input = Contacts.openContactPhotoInputStream(
          context.getContentResolver(),
          ContentUris.withAppendedId(Contacts.CONTENT_URI, cursor.getInt(1)));
      if (input != null) {
        image.setImageBitmap(BitmapFactory.decodeStream(input));
      } else {
        image.setImageBitmap(BitmapFactory.decodeResource(
            context.getResources(), R.drawable.ic_contact_picture));
      }
    } else {
      image.setVisibility(View.GONE);
    }

    TextView name = (TextView) view.findViewById(R.id.receiver_name);
    name.setText(cursor.getString(2));

    TextView type = (TextView) view.findViewById(R.id.receiver_type);
    type.setText(Phone.getTypeLabel(context.getResources(), cursor.getInt(4),
        cursor.getString(5)));

    TextView number = (TextView) view.findViewById(R.id.receiver_number);
    number.setText(cursor.getString(3));
  }

  @Override
  public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
    String constraintPath = null;
    if (constraint != null) {
      constraintPath = constraint.toString();
    }

    Uri queryURI = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI,
        Uri.encode(constraintPath));

    String[] projection = { Phone._ID, Phone.CONTACT_ID, Phone.DISPLAY_NAME,
        Phone.NUMBER, Phone.TYPE, Phone.LABEL, };

    // default: all numbers
    String selection = null;
    String[] selectionArgs = null;
    
    String filter = preferences.getString("filter_receiver", "");

    if (filter.contains("M")) { // mobiles only
      selection = Phone.TYPE + "=? OR " + Phone.TYPE + "=?";
      selectionArgs = new String[] { 
          String.valueOf(Phone.TYPE_MOBILE),
          String.valueOf(Phone.TYPE_WORK_MOBILE)};
    }
    if (filter.contains("H")) { // no home numbers
      selection = Phone.TYPE + "<>?";
      selectionArgs = new String[] { String.valueOf(Phone.TYPE_HOME) };
    }

    String sortOrder = Contacts.TIMES_CONTACTED + " DESC";

    return context.getContentResolver()
        .query(queryURI, projection, selection, selectionArgs, sortOrder);
  }

  @Override
  public CharSequence convertToString(Cursor cursor) {
    SpannableString receiver = new SpannableString(
        cursor.getString(2) + " <" + cursor.getString(3) + ">");

    receiver.setSpan(new Annotation("number", cursor.getString(3)), 0,
        receiver.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    receiver.setSpan(new Annotation("name", cursor.getString(2)), 0,
        receiver.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

    return receiver;
  }

}
