package com.example.nameless.autoupdating;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by nameless on 07.04.18.
 */

public class MessagesAdapter extends ArrayAdapter<Message>  implements Filterable{

    private Context ma;
    private ArrayList<Message> messages;
    private ArrayList<Message> filteredMessageList;

    public MessagesAdapter(Context ma, ArrayList<Message> messages) {
        super(ma, 0, messages);
        this.ma = ma;
        this.messages = messages;
        this.filteredMessageList = messages;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(filteredMessageList.size() > 0) {
            LayoutInflater li = (LayoutInflater)ma.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            convertView = li.inflate(R.layout.message_item, parent, false);

            if((filteredMessageList.get(position).getWho())
                    .equals(Authentification.myAcc.getLogin())) {
                (convertView.findViewById(R.id.content))
                        .setBackgroundResource(R.drawable.in_message_bg);

                RelativeLayout.LayoutParams layoutParams =
                        (RelativeLayout.LayoutParams) convertView.findViewById(
                                R.id.content).getLayoutParams();
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

                layoutParams = (RelativeLayout.LayoutParams) convertView.findViewById(
                                R.id.tvDate).getLayoutParams();
                layoutParams.addRule(RelativeLayout.LEFT_OF, R.id.content);

                if(filteredMessageList.get(position).getFileUrl() != null) {
                    layoutParams = (RelativeLayout.LayoutParams) convertView.findViewById(
                                    R.id.ivImage).getLayoutParams();
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                }
            } else {
                (convertView.findViewById(R.id.content))
                        .setBackgroundResource(R.drawable.out_message_bg);

                RelativeLayout.LayoutParams layoutParams =
                        (RelativeLayout.LayoutParams) convertView.findViewById(
                                R.id.content).getLayoutParams();

                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

                layoutParams = (RelativeLayout.LayoutParams) convertView.findViewById(
                        R.id.tvDate).getLayoutParams();
                layoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.content);

                if(filteredMessageList.get(position).getFileUrl() != null) {
                    layoutParams = (RelativeLayout.LayoutParams) convertView.findViewById(
                            R.id.ivImage).getLayoutParams();
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                }
            }

//            if(filteredMessageList.get(position).getFileUrl() != null) {
//                ((ImageView)convertView.findViewById(R.id.ivImage))
//                        .setImageURI(filteredMessageList.get(position).getFileUrl());
//            }

            ((TextView)convertView.findViewById(R.id.tvContent))
                .setText(filteredMessageList.get(position).getContent());

            DateFormat dateFormat = (new SimpleDateFormat("HH:mm:ss \n dd MMM"));
            ((TextView)convertView.findViewById(R.id.tvDate))
                    .setText(dateFormat.format(filteredMessageList
                            .get(position).getDateOfSend()));
        }
        return convertView;
    }

    @Override
    public int getCount() {
        return filteredMessageList.size();
    }

    @NonNull
    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults result = new FilterResults();
                ArrayList<Message> filteredList  = new ArrayList<>();
                constraint = constraint.toString().toLowerCase();
                for(int i = 0; i < messages.size(); i++) {
                    if((messages.get(i).getContent()).toLowerCase()
                            .contains(constraint.toString())) {
                        filteredList.add(messages.get(i));
                    }
                }
                result.count = filteredList.size();
                result.values = filteredList;
                return result;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredMessageList = (ArrayList<Message>)results.values;
                if (filteredMessageList.size() > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }
}
