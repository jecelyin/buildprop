package com.jecelyin.buildprop;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

/**
 * @author Jecelyin
 */
public class ListItemAdapter extends BaseAdapter {
    private final SharedPreferences sharedPref;
    private  String[] menuItems;

    public ListItemAdapter(Context context) {
        sharedPref = context.getSharedPreferences(Common.PACKAGE_PREFERENCES, Context.MODE_WORLD_READABLE);
    }

    public void setData(String[] menuItems) {
        this.menuItems = menuItems;
    }

    @Override
    public int getCount() {
        return menuItems == null ? 0 : menuItems.length;
    }

    @Override
    public Object getItem(int position) {
        return menuItems[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CheckedTextView ctv;
        if(convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_checked, parent, false);
        }
        ctv = (CheckedTextView) convertView;
        String text = menuItems[position];
        ctv.setText(text);
        ctv.setChecked(sharedPref.getBoolean(text, false));
        return convertView;
    }
}
