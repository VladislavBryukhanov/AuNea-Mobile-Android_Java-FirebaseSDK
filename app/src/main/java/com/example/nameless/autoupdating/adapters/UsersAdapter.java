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
import com.example.nameless.autoupdating.asyncTasks.DownloadAvatarByUrl;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.models.Dialog;
import com.example.nameless.autoupdating.models.User;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by nameless on 07.04.18.
 */

public class UsersAdapter extends ArrayAdapter<Dialog>  implements Filterable{

    private Context ma;
    private ArrayList<Dialog> users;
    private ArrayList<Dialog> filteredUserList;

    public UsersAdapter(Context ma, ArrayList<Dialog> users) {
        super(ma, 0, users);
        this.ma = ma;
        this.users = users;
        this.filteredUserList = users;
        mAuth = FirebaseAuth.getInstance();
    }

    private FirebaseAuth mAuth;

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(filteredUserList.size() > 0) {
            LayoutInflater li = (LayoutInflater)ma.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            convertView = li.inflate(R.layout.user_item, parent, false);

            ((TextView)convertView.findViewById(R.id.tvLogin))
                    .setText(filteredUserList.get(position).getSpeaker().getLogin());
           /* if(filteredUserList.get(position).getNickname() != null  && filteredUserList.get(position).getNickname().length() > 0) {
                ((TextView)convertView.findViewById(R.id.tvNickname)).setText('@'+filteredUserList.get(position).getNickname());
            }*/

            Dialog dialog = filteredUserList.get(position);
            int messageCounter = dialog.getUnreadCounter();
            String sender = dialog.getLastMessage().getWho();
            TextView msgCounter = (convertView.findViewById(R.id.msgCounter));
            TextView lastMsg = (convertView.findViewById(R.id.tvLastMsg));
            TextView tvLastMsgTime = (convertView.findViewById(R.id.tvLastMsgTime));

            if (dialog.getUnreadCounter() > 0 && !sender.equals(mAuth.getUid())) {
                msgCounter.setText(String.valueOf(messageCounter));
                msgCounter.setVisibility(View.VISIBLE);
            } else {
                msgCounter.setVisibility(View.GONE);
            }

            if (!sender.equals(mAuth.getUid())) {
                lastMsg.setTextColor(ContextCompat.getColor(ma, R.color.outcomeMessage));
            } else {
                lastMsg.setTextColor(ContextCompat.getColor(ma, R.color.black_overlay));
            }
            lastMsg.setText(dialog.getLastMessage().getContent());

            Date dateOfSend = dialog.getLastMessage().getDateOfSend();
            DateFormat dateFormat = (new SimpleDateFormat("HH:mm:ss"));
            if (dateOfSend.getDay() != new Date().getDay()) {
                dateFormat = (new SimpleDateFormat("dd MMM"));
            }
            tvLastMsgTime.setText(dateFormat.format(dateOfSend));

            // Прекратить пересечивание
            if(filteredUserList.get(position).getSpeaker().getAvatarUrl() != null) {
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
                        .load(filteredUserList.get(position).getSpeaker().getAvatarUrl())
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
                        if((users.get(i).getSpeaker().getNickname()).toLowerCase()
                                .contains(constraint.toString())) {
                            filteredList.add(users.get(i).getSpeaker());
                        }
                    }
                } else {
                    for(int i = 0; i < users.size(); i++) {
                        if((users.get(i).getSpeaker().getLogin()).toLowerCase()
                                .contains(constraint.toString())) {
                            filteredList.add(users.get(i).getSpeaker());
                        }
                    }
                }

                result.count = filteredList.size();
                result.values = filteredList;
                return result;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredUserList = (ArrayList<Dialog>)results.values;
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
