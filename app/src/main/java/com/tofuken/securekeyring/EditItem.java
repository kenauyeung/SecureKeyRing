package com.tofuken.securekeyring;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class EditItem extends AppCompatActivity {
    ItemBean editItemBean = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);

        Intent intent = getIntent();
        int pos = intent.getIntExtra(MainActivity.ITEM_CLICK_POS, -1);
        if (pos > -1) {
            editItemBean = MainActivity.keyContainer.list.get(pos);
            if (editItemBean != null) {
                EditText editName = (EditText) findViewById(R.id.edit_name);
                EditText editKey = (EditText) findViewById(R.id.edit_key);
                EditText editComment = (EditText) findViewById(R.id.edit_comment);

                editName.setText(editItemBean.name);
                editKey.setText(editItemBean.key);
                editComment.setText(editItemBean.comment);
            }
            Log.d("Data", "At Edit:" + MainActivity.keyContainer.list.get(pos).toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("OutDebug", "Calling onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_menu, menu);

        Log.d("OutDebug", "menu : " + R.menu.edit_menu);
        // return super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_cancel:
                finish();
                return true;
            case R.id.action_save:
                toSave();
                return true;
        }
        return false;
    }


    public void toSave() {


        EditText editName = (EditText) findViewById(R.id.edit_name);
        EditText editKey = (EditText) findViewById(R.id.edit_key);
        EditText editComment = (EditText) findViewById(R.id.edit_comment);

        String n =  editName.getText().toString();
        String k = editKey.getText().toString();

        if(n.isEmpty() || k.isEmpty()){
            Utils.popAlert(EditItem.this, R.string.info_title, R.string.msg_name_key_not_empty);
        }else {
            Log.d("Data", "Item : " + editItemBean);
            if (editItemBean == null) {
                editItemBean = new ItemBean();
                MainActivity.keyContainer.addItem(editItemBean);
            }


            editItemBean.name = n;
            editItemBean.key = k;
            editItemBean.comment = editComment.getText().toString();
            editItemBean = null;

            Intent myIntent = new Intent(this, MainActivity.class);
            myIntent.putExtra(MainActivity.ITEM_SAVE, true); //Optional parameters
            setResult(RESULT_OK, myIntent);
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}
