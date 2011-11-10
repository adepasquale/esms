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

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.awsms.account.AccountManagerAndroid;
import com.googlecode.awsms.R;
import com.googlecode.esms.account.Account;
import com.googlecode.esms.account.AccountManager;

public class AccountDisplayActivity extends Activity {

  AccountManager accountManager;

  LinearLayout accountsLinear;
  Button createButton;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.account_display_activity);

    accountManager = new AccountManagerAndroid(AccountDisplayActivity.this);
    if (accountManager.getAccounts().size() == 0) showWelcomeDialog();

    accountsLinear = (LinearLayout) findViewById(R.id.accounts_linear);
    refreshAccountsList();
  }

  private void refreshAccountsList() {
    accountsLinear.removeAllViews();
    List<Account> accounts = accountManager.getAccounts();
    
    for (final Account account : accounts) {
      View listItem = getLayoutInflater().inflate(
          R.layout.account_display_list_item, null);
      LinearLayout listItemLinear = (LinearLayout) listItem
          .findViewById(R.id.list_item_linear);
      TextView listItemLabel = (TextView) listItem
          .findViewById(R.id.list_item_label);
      TextView listItemSender = (TextView) listItem
          .findViewById(R.id.list_item_sender);
      ImageView listItemLogo = (ImageView) listItem
          .findViewById(R.id.list_item_logo);

      String label = account.getLabel();
      if (label == null || label.equals(""))
        label = getString(R.string.no_label_text);
      listItemLabel.setText(label);
      listItemSender.setText(account.getSender());
      
      if (account.getProvider().equals("Telefono")) {
//        listItemLogo.setImageBitmap(
//            BitmapFactory.decodeResource(getResources(),
//            R.drawable.ic_logo_SIM));
      }
      
      if (account.getProvider().equals("Vodafone")) {
        listItemLogo.setImageBitmap(
            BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_logo_vodafone));
      }
        
      if (account.getProvider().equals("TIM")) { 
        listItemLogo.setImageBitmap(
            BitmapFactory.decodeResource(getResources(), 
            R.drawable.ic_logo_tim));
      }
      
      if (account.getProvider().equals("Tre")) {
//        listItemLogo.setImageBitmap(
//            BitmapFactory.decodeResource(getResources(), 
//            R.drawable.ic_logo_tre));
      }
      
      listItemLinear.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          showModifyActivity(account);
        }
      });

      listItemLinear.setOnLongClickListener(new OnLongClickListener() {
        public boolean onLongClick(View v) {
          showAccountDialog(account);
          return true;
        }
      });

      accountsLinear.addView(listItem);
    }

    if (accounts.size() == 0) {
      View listEmpty = getLayoutInflater().inflate(
          R.layout.account_display_list_empty, null);
      accountsLinear.addView(listEmpty);
    }
    
    accountsLinear.invalidate();
  }

  private void showAccountDialog(final Account account) {
    AlertDialog.Builder builder = new AlertDialog.Builder(
        AccountDisplayActivity.this);

    Resources resources = getResources();
    CharSequence[] items = resources.getTextArray(R.array.account_dialog);

    String label = account.getLabel();
    if (label == null || label.equals(""))
      label = getString(R.string.no_label_text);

    builder.setTitle(label);
    builder.setItems(items, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int item) {
        switch (item) {
        case 0: // rename
          showRenameDialog(account);
          break;

        case 1: // modify
          showModifyActivity(account);
          break;

        case 2: // delete
          showDeleteDialog(account);
          break;
        }
      }
    });

    builder.show();
  }

  private void showRenameDialog(final Account account) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);

    LayoutInflater inflater = (LayoutInflater) this
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    LinearLayout renameLinear = (LinearLayout) inflater.inflate(
        R.layout.account_rename_dialog, null);
    final EditText labelText = (EditText) renameLinear
        .findViewById(R.id.label_text);

    String label = account.getLabel();
    labelText.setText(label);

    if (label == null || label.equals(""))
      label = getString(R.string.no_label_text);

    builder.setTitle(label);
    builder.setView(renameLinear);

    builder.setPositiveButton(R.string.rename_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            String oldLabel = account.getLabel();
            String newLabel = labelText.getText().toString();
            if (newLabel.equalsIgnoreCase(oldLabel)) return;
            
            for (Account a : accountManager.getAccounts())
              if (a.getLabel().equalsIgnoreCase(newLabel)) {
                Toast.makeText(AccountDisplayActivity.this, 
                    R.string.existing_label_toast, Toast.LENGTH_SHORT).show();
                showRenameDialog(account);
                return;
              }
            
            account.setLabel(newLabel);
            accountManager.update(oldLabel, account);
            refreshAccountsList();
          }
        });

    builder.setNegativeButton(R.string.cancel_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            // do nothing
          }
        });

    builder.show();
    labelText.setSelection(0, labelText.length());
    labelText.requestFocus();
  }

  private void showModifyActivity(Account account) {
    Intent intent = new Intent(AccountDisplayActivity.this,
        AccountModifyActivity.class);
    intent.setAction(AccountIntents.DO_AUTHENTICATION);
    intent.putExtra(AccountIntents.NEW_ACCOUNT, account);
    intent.putExtra(AccountIntents.OLD_ACCOUNT, account);
    startActivity(intent);
  }

  private void showDeleteDialog(final Account account) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);

    String label = account.getLabel();
    if (label == null || label.equals(""))
      label = getString(R.string.no_label_text);

    builder.setTitle(label);
    builder.setMessage(R.string.account_delete_message);

    builder.setPositiveButton(R.string.yes_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            accountManager.delete(account);
            refreshAccountsList();
          }
        });

    builder.setNegativeButton(R.string.no_button,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            // do nothing
          }
        });

    builder.show();
  }

  @Override
  public void onBackPressed() {
    if (accountManager.getAccounts().size() == 0)
      showWelcomeDialog();
    else super.onBackPressed();
  }
  
  private void showWelcomeDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.app_name);
    builder.setMessage(R.string.welcome_message);

    builder.setPositiveButton(
        R.string.account_create_button, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        startActivity(new Intent(
            AccountDisplayActivity.this,
            AccountCreateActivity.class));
      }
    });

    builder.setCancelable(false);
    builder.show();
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.layout.create_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.menu_item_create:
      startActivity(new Intent(
          AccountDisplayActivity.this,
          AccountCreateActivity.class));
      return true;

    default:
      return super.onOptionsItemSelected(item);
    }
  }
}
