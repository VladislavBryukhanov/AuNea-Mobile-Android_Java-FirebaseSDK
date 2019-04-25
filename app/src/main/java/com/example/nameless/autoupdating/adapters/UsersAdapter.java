package com.example.nameless.autoupdating.adapters;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.common.NetworkUtil;
import com.example.nameless.autoupdating.models.User;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by nameless on 07.04.18.
 */

public class UsersAdapter extends ArrayAdapter<User>  implements Filterable {

    private Context ma;
    private ArrayList<User> users;
    private ArrayList<User> filteredUserList;

    public UsersAdapter(Context ma, ArrayList<User> users) {
        super(ma, 0, users);
        this.ma = ma;
        this.users = users;
        this.filteredUserList = users;
    }


    // TODO Dialogs adapter must extends user adapter
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(filteredUserList.size() > 0) {
            LayoutInflater li = (LayoutInflater)ma.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            convertView = li.inflate(R.layout.user_item, parent, false);
            User currentUser = filteredUserList.get(position);

            ((TextView)convertView.findViewById(R.id.tvLogin))
                    .setText(currentUser.getLogin());

            if(currentUser.getNickname() != null && currentUser.getNickname().length() > 0) {
                TextView nickname = convertView.findViewById(R.id.tvNickname);
                nickname.setVisibility(View.VISIBLE);
                nickname.setText('@'+currentUser.getNickname());
            }

            String userStatus = currentUser.getStatus();
            if (userStatus.contains(NetworkUtil.ONLINE_STATUS)) {
                convertView.findViewById(R.id.onlineStatus)
                        .setBackground(ContextCompat.getDrawable(getContext(), R.drawable.network_status_online));
            } else if (userStatus.contains(NetworkUtil.AFK_STATUS)) {
                convertView.findViewById(R.id.onlineStatus)
                        .setBackground(ContextCompat.getDrawable(getContext(), R.drawable.network_status_afk));
            //AFK contains OFFLINE_STATUS contains, but OFFLINE_STATUS contains not AFK_STATUS
            } else if (userStatus.contains(NetworkUtil.OFFLINE_STATUS)) {
                convertView.findViewById(R.id.onlineStatus)
                        .setBackground(ContextCompat.getDrawable(getContext(), R.drawable.network_status_offline));
            } else {
            // Exception - may be able if user use old app version
                convertView.findViewById(R.id.onlineStatus)
                        .setBackground(ContextCompat.getDrawable(getContext(), R.drawable.network_status_offline));
            }

            // Прекратить пересечивание
            if(currentUser.getAvatarUrl() != null) {
           /*     DownloadAvatarByUrl downloadTask = new DownloadAvatarByUrl(
                        convertView.findViewById(R.id.profile_image),
                        filteredUserList.get(position).getSpeaker(),
                        ma,
                        BitmapFactory.decodeResource(getContext().getResources(), R.drawable.avatar));
                try {
                    downloadTask.execute(filteredUserList.get(position).getSpeaker().getAvatarUrl()).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }*/
                CircleImageView avatar = convertView.findViewById(R.id.profile_image);
                Glide.with(getContext())
                        .load(currentUser.getAvatarUrl())
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.avatar))
                        .into(avatar);
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
