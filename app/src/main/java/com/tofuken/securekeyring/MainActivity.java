package com.tofuken.securekeyring;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Base64;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    final static int REQUEST_SAVE = 99;
    //public final static String EXTRA_MESSAGE = "com.tofuken.securekeyring.MESSAGE";
    public final static String ITEM_CLICK_POS = "com.tofuken.securekeyring.MainActivity.pos";
    public final static String ITEM_SAVE = "com.tofuken.securekeyring.MainActivity.save";
    final static KeyContainer keyContainer = new KeyContainer();

    private Menu mainMenu = null;
    private long lastStop = 0;
    private long idleReset = 60000; // when app idle for 1 min it will reset


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // ActionBar actionBar = this.getSupportActionBar();
        // actionBar.setTitle("這是ActionBar標題");
        // ActionBar actionBar = this.getSupportActionBar();
        // actionBar.setTitle(R.string.app_name);
        //Utils.popAlert(this, "Test", this.getResources().getString(R.string.app_name));

        ListView list = (ListView) findViewById(R.id.key_list);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                gotoEditPage(i);
            }
        });
        Log.d("Data", "Calling onCreate");
    }

    private void gotoEditPage(int itemPost) {

        Intent myIntent = new Intent(MainActivity.this, EditItem.class);
        if (itemPost > -1) {
            //Log.d("Data", keyContainer.list.get(itemPost).toString());
            myIntent.putExtra(ITEM_CLICK_POS, itemPost); //Optional parameters
        }
        MainActivity.this.startActivityForResult(myIntent, REQUEST_SAVE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Data", "Calling onActivityResult : " + requestCode + "," + resultCode + ", " + (data == null));
        if (requestCode == REQUEST_SAVE && resultCode == RESULT_OK && data != null) {
            boolean toSave = data.getBooleanExtra(ITEM_SAVE, false);
            Log.d("Data", "Calling Save : " + toSave + ", Byte Null?:" + (keyContainer.keyByte == null) + ", Container:" + keyContainer);
            if (toSave && keyContainer.keyByte != null) {
                saveContainer(keyContainer.keyFilename, keyContainer.keyByte, false, true);
                reloadContainer();
            }
        }
    }

    private void reloadContainer() {
        TextView viewTitle = (TextView) findViewById(R.id.titleSection);
        loadContainer(viewTitle, keyContainer.keyFilename, keyContainer.keyByte);
    }

    @Override
    protected void onResume() {
        Log.d("Data", "Calling onresume");

        if (lastStop > 0 && System.currentTimeMillis() - lastStop > idleReset) {
            reset();
        }
        lastStop = 0;

        if (keyContainer.keyByte == null && Utils.haveSavedFile(this)) {
            loadSecureFileDialog();
        }
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d("Data", "Calling onStop");
        //reset();
        lastStop = System.currentTimeMillis();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Log.d("OutDebug", "Calling onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        mainMenu = menu;
        //Log.d("OutDebug", "menu : " + R.menu.main_menu);
        // return super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_load_file:
                loadSecureFileDialog();
                return true;
            case R.id.action_new_file:
                createSecureFileDialog();
                return true;
            case R.id.action_new_key:
                if (keyContainer.keyByte != null) {
                    gotoEditPage(-1);
                } else {
                    Utils.popAlert(this, R.string.info_title, R.string.msg_load_file);
                }
                return true;
            case R.id.action_del_key:
                showItemDelCheckbox(true);
                return true;
            case R.id.action_del_confirm:
                askDelConfirm();
                showItemDelCheckbox(false);
                return true;
            case R.id.action_changekey:
                loadChangeKeyDialog();
                return true;
           /* case R.id.action_setting:
                Intent myIntent = new Intent(MainActivity.this, Settings.class);
                MainActivity.this.startActivity(myIntent);
                return true;*/
        }
        return false;
    }

    private void loadChangeKeyDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.change_key_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);

        /*
        By making lastPrice and price final, they are not really variables anymore, but constants. The compiler can then
        just replace the use of lastPrice and price in the anonymous class with the values of the constants (at compile time, ofcourse)
        , and you won't have the problem with accessing non-existent variables anymore.
         */
        final EditText currentKeyTxt = (EditText) promptView.findViewById(R.id.old_key);
        final EditText keyTxt = (EditText) promptView.findViewById(R.id.unlock_key);
        final EditText confirmKeyTxt = (EditText) promptView.findViewById(R.id.confirm_key);
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String oldKeyStr = currentKeyTxt.getText().toString();
                        String newKeyStr = keyTxt.getText().toString();
                        String confirmKeyStr = confirmKeyTxt.getText().toString();


                        if (newKeyStr.isEmpty()) {
                            Utils.popAlert(MainActivity.this, R.string.info_title, R.string.field_cannot_empty);
                        } else if (!newKeyStr.equals(confirmKeyStr)) {
                            Utils.popAlert(MainActivity.this, R.string.info_title, R.string.field_key_not_match);
                        } else {

                            // check current key before allow change to new key
                            int pass = 0;
                            byte[] oldKeyByte = oldKeyStr.getBytes();
                            if (oldKeyByte.length == keyContainer.keyByte.length) {
                                for (int i = 0; i < oldKeyByte.length; i++) {
                                    if (oldKeyByte[i] == keyContainer.keyByte[i]) {
                                        pass++;
                                    }
                                }
                            } else {
                                Log.d("Data", "Unmatch key:" + oldKeyByte.length + "," + keyContainer.keyByte.length);
                            }

                            if (pass != keyContainer.keyByte.length) {
                                Utils.popAlert(MainActivity.this, R.string.info_title, R.string.field_old_key_not_match);
                            } else {
                                if(saveContainer(keyContainer.keyFilename, newKeyStr.getBytes(), false, true)){
                                    Utils.popAlert(MainActivity.this, R.string.info_title, R.string.msg_key_changed);
                                }
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.button_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();

    }

    private void askDelConfirm() {

        ListView list = (ListView) MainActivity.this.findViewById(R.id.key_list);
        boolean isChanged = false;

        ArrayAdapter adapter = (ArrayAdapter) list.getAdapter();
        for (int i = adapter.getCount() - 1; i >= 0; i--) {
            View v = list.getChildAt(i);
            CheckBox check = (CheckBox) v.findViewById(R.id.item_checkbox);
            if (check != null && check.isChecked()) {
                isChanged = true;
            }
        }

        if (isChanged) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            Resources res = MainActivity.this.getResources();
            builder.setTitle(res.getString(R.string.confirm_title));
            builder.setMessage(res.getString(R.string.msg_confirm_del));
            builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    confirmItemDel();
                }
            });

            builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    reloadContainer();
                    // Do nothing
                    dialog.dismiss();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
        } else {
            showItemDelCheckbox(false);
        }
    }

    private void confirmItemDel() {
        ListView list = (ListView) MainActivity.this.findViewById(R.id.key_list);
        boolean isChanged = false;

        ArrayAdapter adapter = (ArrayAdapter) list.getAdapter();
        for (int i = adapter.getCount() - 1; i >= 0; i--) {
            View v = list.getChildAt(i);
            CheckBox check = (CheckBox) v.findViewById(R.id.item_checkbox);
            if (check != null && check.isChecked()) {
                adapter.remove(adapter.getItem(i));
                check.setChecked(false);
                isChanged = true;
            }
        }
        if (isChanged) {
            //adapter.notifyDataSetChanged();
            saveContainer(keyContainer.keyFilename, keyContainer.keyByte, false, true);
            reloadContainer();
        }
    }

    private void showItemDelCheckbox(boolean show) {
        // Utils.popAlert(this, "Info", "Not Implement yet");
        ListView list = (ListView) findViewById(R.id.key_list);
        int vis = show ? View.VISIBLE : View.INVISIBLE;
        for (int i = 0; i < list.getAdapter().getCount(); i++) {
            View v = list.getChildAt(i);
            CheckBox check = (CheckBox) v.findViewById(R.id.item_checkbox);
            if (check != null) {
                check.setVisibility(vis);
            }
        }
        mainMenu.findItem(R.id.action_del_confirm).setVisible(show);
    }

    private void loadSecureFileDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.dropdown_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);

        /*
        By making lastPrice and price final, they are not really variables anymore, but constants. The compiler can then
        just replace the use of lastPrice and price in the anonymous class with the values of the constants (at compile time, ofcourse)
        , and you won't have the problem with accessing non-existent variables anymore.
         */
        final EditText keyTxt = (EditText) promptView.findViewById(R.id.unlock_key);
        final Spinner comboBox = (Spinner) promptView.findViewById(R.id.unlock_key_file);
        final TextView viewTitle = (TextView) findViewById(R.id.titleSection);

        String[] fList = Utils.getFileList(this);

        if (fList != null && fList.length > 0) {
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, fList);
            comboBox.setAdapter(arrayAdapter);

            // setup a dialog window
            alertDialogBuilder.setCancelable(false)
                    .setPositiveButton(R.string.button_unlock, new DialogInterface.OnClickListener() {


                        public void onClick(DialogInterface dialog, int id) {
                            if (comboBox.getSelectedItem() != null) {
                                String filename = comboBox.getSelectedItem().toString();
                                String keyStr = keyTxt.getText().toString();

                                if (!keyStr.isEmpty()) {
                                    byte[] keyByte = keyStr.getBytes();
                                    loadContainer(viewTitle, filename, keyByte);
                                } else {
                                    Utils.popAlert(MainActivity.this, R.string.info_title, R.string.field_cannot_empty);
                                }
                            }
                        }
                    })
                    .setNegativeButton(R.string.button_cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

            // create an alert dialog
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
        } else {
            Utils.popAlert(MainActivity.this, R.string.info_title, R.string.msg_no_file_list);
        }
    }

    private void loadContainer(TextView viewTitle, String filename, byte[] keyByte) {
        try {
            byte[] data = Utils.readFile(MainActivity.this, filename, keyByte);
            if (data != null) {
                keyContainer.loadList(data);
                updateKeyContainer(viewTitle, Utils.getFileObject(MainActivity.this, filename).toString(), keyByte);
                refreshList();

                // enable add/del key item
                showKeyMenu(true);

                Log.d("Data", "Load key Container:" + keyContainer);
            } else {
                Utils.popAlert(MainActivity.this, R.string.error_title, R.string.msg_error_load);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Utils.popAlert(MainActivity.this, "Exception", e.getMessage());
        } catch (InvalidCipherTextException e) {
            Utils.popAlert(MainActivity.this, R.string.error_title, R.string.invalid_key);
        }
    }

    private void createSecureFileDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.new_key_file_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);

        final EditText editFilename = (EditText) promptView.findViewById(R.id.new_key_file);
        final EditText editKeyStr = (EditText) promptView.findViewById(R.id.unlock_key);
        final EditText editConfirmKeyStr = (EditText) promptView.findViewById(R.id.confirm_key);

        final CheckBox chkOverwrite = (CheckBox) promptView.findViewById(R.id.chk_overwrite);


        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton(R.string.button_create, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        String filename = editFilename.getText().toString().trim();
                        String key = editKeyStr.getText().toString();
                        String confirmKey = editConfirmKeyStr.getText().toString();

                        if (filename.isEmpty() || key.isEmpty()) {
                            Utils.popAlert(MainActivity.this, R.string.info_title, R.string.field_cannot_empty);
                        } else if (!key.equals(confirmKey)) {
                            Utils.popAlert(MainActivity.this, R.string.info_title, R.string.field_key_not_match);
                        } else {
                            saveContainer(filename, key.getBytes(), true, chkOverwrite.isChecked());
                            showKeyMenu(true);
                            refreshList();
                        }
                    }
                })
                .setNegativeButton(R.string.button_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private boolean saveContainer(String filename, byte[] keyByte, boolean newContainer, boolean overwrite) {
        if (newContainer || keyContainer.keyByte != null) {
            try {
                TextView viewTitle = (TextView) findViewById(R.id.titleSection);
                String f = Utils.saveFile(MainActivity.this, filename, keyByte,
                        keyContainer.getBytes(newContainer),
                        overwrite);
                if (!f.isEmpty()) {
                    if (newContainer) {
                        reset();
                    }
                    updateKeyContainer(viewTitle, f, keyByte);
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Utils.popAlert(MainActivity.this, "Exception", e.getMessage());
            } catch (InvalidCipherTextException e) {
                Utils.popAlert(MainActivity.this, R.string.error_title, R.string.invalid_key);
            }
        } else {
            Utils.popAlert(MainActivity.this, R.string.error_title, R.string.invalid_key);
        }
        return false;
    }

    private void updateKeyContainer(TextView viewTitle, String f, byte[] keyByte) {
        viewTitle.setText(f);
        keyContainer.keyFilename = f;
        keyContainer.keyByte = keyByte;
    }


    // on/off for menu item only available when key file is loaded
    void showKeyMenu(boolean show) {
        mainMenu.findItem(R.id.action_new_key).setVisible(show);
        mainMenu.findItem(R.id.action_del_key).setVisible(show);
        mainMenu.findItem(R.id.action_changekey).setVisible(show);
    }

    void refreshList() {
        ListView list = (ListView) findViewById(R.id.key_list);
        //Log.d("Data",keyContainer.list.toString());
        list.setAdapter(new KeyListItemAdapter(this, R.layout.key_list_item, keyContainer.list));
    }

    void reset() {
        keyContainer.reset();
        ListView list = (ListView) findViewById(R.id.key_list);
        ((KeyListItemAdapter) list.getAdapter()).notifyDataSetChanged();
        TextView viewTitle = (TextView) findViewById(R.id.titleSection);
        viewTitle.setText("");
        showKeyMenu(false);
    }


}
