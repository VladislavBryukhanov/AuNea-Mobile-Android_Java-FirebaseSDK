package com.example.nameless.autoupdating.adapters;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.nameless.autoupdating.asyncTasks.DownloadAvatarByUrl;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.models.User;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import de.hdodenhof.circleimageview.CircleImageView;

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
            if(filteredUserList.get(position).getNickname() != null  && filteredUserList.get(position).getNickname().length() > 0) {
                ((TextView)convertView.findViewById(R.id.tvNickname)).setText('@'+filteredUserList.get(position).getNickname());
            }
            if(filteredUserList.get(position).getAvatarUrl() != null) {
                DownloadAvatarByUrl downloadTask = new DownloadAvatarByUrl(
                        (ImageView)convertView.findViewById(R.id.profile_image),
                        filteredUserList.get(position),
                        ma,
                        BitmapFactory.decodeResource(getContext().getResources(), R.drawable.avatar));
                try {
                    downloadTask.execute(filteredUserList.get(position).getAvatarUrl()).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
/*                CircleImageView avatar = convertView.findViewById(R.id.profile_image);
                Glide.with(getContext())
                        .load(filteredUserList.get(position).getAvatarUrl())
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.avatar))
                        .into(avatar);*/
            }

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
                if( constraint.length() > 0 && constraint.toString().substring(0,1).equals("@")) {
                    constraint = constraint.subSequence(1, constraint.length());
                    for(int i = 0; i < users.size(); i++) {
                        if((users.get(i).getNickname()).toLowerCase()
                                .contains(constraint.toString())) {
                            filteredList.add(users.get(i));
                        }
                    }
                } else {
                    for(int i = 0; i < users.size(); i++) {
                        if((users.get(i).getLogin()).toLowerCase()
                                .contains(constraint.toString())) {
                            filteredList.add(users.get(i));
                        }
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
