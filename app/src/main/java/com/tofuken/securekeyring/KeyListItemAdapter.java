package com.tofuken.securekeyring;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Ken on 8/2/2016.
 */
public class KeyListItemAdapter extends ArrayAdapter<ItemBean> {
    public KeyListItemAdapter(Context context, int resource, List items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.key_list_item, null);
        }

        ItemBean p = getItem(position);

        if (p != null) {
            TextView tt1 = (TextView) v.findViewById(R.id.item_name);

            if (tt1 != null) {
                tt1.setText(p.name);
            }

        }

        return v;
    }
}