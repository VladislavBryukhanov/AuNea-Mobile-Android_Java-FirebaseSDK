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
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by nameless on 07.04.18.
 */

public class UsersAdapter extends ArrayAdapter<User>  implements Filterable{

    private Context ma;
    private ArrayList<User> users;
    private ArrayList<User> filteredUserList;

    public UsersAdapter(Context ma, ArrayList<User> users) {
        super(ma, 0, users);
        this.ma = ma;
        this.users = users;
        this.filteredUserList = users;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(filteredUserList.size() > 0) {
//            (convertView.getLayoutParams()).height = 20;
            LayoutInflater li = (LayoutInflater)ma.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            convertView = li.inflate(R.layout.user_item, parent, false);
            ((TextView)convertView.findViewById(R.id.tvLogin)).setText(filteredUserList.get(position).getLogin());
        }
        return convertView;
    }

    @Override
    public int getCount() {
        return filteredUserList.size();
    }

    @NonNull
    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults result = new FilterResults();
                ArrayList<User> filteredList  = new ArrayList<>();
                constraint = constraint.toString().toLowerCase();
                for(int i = 0; i < users.size(); i++) {
                    if((users.get(i).getLogin()).toLowerCase()
                            .contains(constraint.toString())) {
                        filteredList.add(users.get(i));
                    }
                }
                result.count = filteredList.size();
                result.values = filteredList;
                return result;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredUserList = (ArrayList<User>)results.values;
                if (filteredUserList.size() > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }
}
