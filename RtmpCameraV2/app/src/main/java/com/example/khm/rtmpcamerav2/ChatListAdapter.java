package com.example.khm.rtmpcamerav2;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by khm on 2017-06-30.
 */

public class ChatListAdapter extends BaseAdapter {

    volatile List<ChatListItem> chatListItems = new ArrayList<>();

    @Override
    public int getCount() {
        return chatListItems.size();
    }

    @Override
    public Object getItem(int position) {
        return chatListItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        final Context context = parent.getContext();

        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.chat_item, parent, false);
        }

        TextView chatItemTextView = (TextView)convertView.findViewById(R.id.chatItem);
        ChatListItem chatItem = chatListItems.get(position);

        chatItemTextView.setText(chatItem.getChatId() + "\n" + chatItem.getChatText());
        return convertView;
    }
    public void addItem(String id, String text){
        ChatListItem item = new ChatListItem();
        item.setChatId(id);
        item.setChatText(text);
        chatListItems.add(item);
    }
}
