package com.example.nameless.autoupdating.adapters;

import android.content.Context;
import android.graphics.Typeface;
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
import com.example.nameless.autoupdating.models.Dialog;
import com.example.nameless.autoupdating.models.Message;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class DialogsAdapter  extends ArrayAdapter<Dialog> implements Filterable {

    private Context ma;
    private ArrayList<Dialog> dialogs;
    private ArrayList<Dialog> filteredDialogList;

    public DialogsAdapter(Context ma, ArrayList<Dialog> dialogs) {
        super(ma, 0, dialogs);
        this.ma = ma;
        this.dialogs = dialogs;
        this.filteredDialogList = dialogs;
        mAuth = FirebaseAuth.getInstance();
    }

    private FirebaseAuth mAuth;

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(filteredDialogList.size() > 0) {
            LayoutInflater li = (LayoutInflater)ma.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            convertView = li.inflate(R.layout.user_item, parent, false);


            ((TextView)convertView.findViewById(R.id.tvLogin))
                    .setText(filteredDialogList.get(position).getSpeaker().getLogin());
           /* if(filteredUserList.get(position).getNickname() != null  && filteredUserList.get(position).getNickname().length() > 0) {
                ((TextView)convertView.findViewById(R.id.tvNickname)).setText('@'+filteredUserList.get(position).getNickname());
            }*/

            Dialog dialog = filteredDialogList.get(position);
            Long unseenCounter = dialog.getInterlocutors().get(mAuth.getUid());
            Message lastMessage = dialog.getLastMessage();
            String sender = lastMessage.getWho();

            TextView msgCounter = (convertView.findViewById(R.id.msgCounter));
            TextView lastMsg = (convertView.findViewById(R.id.tvLastMsg));
            TextView tvLastMsgTime = (convertView.findViewById(R.id.tvLastMsgTime));

            String userStatus = dialog.getSpeaker().getStatus();
            int statusIcon = R.drawable.network_status_offline;

            if (userStatus.contains(NetworkUtil.ONLINE_STATUS)) {
                statusIcon = R.drawable.network_status_online;
            } else if (userStatus.contains(NetworkUtil.AFK_STATUS)) {
                statusIcon = R.drawable.network_status_afk;
            }
            convertView.findViewById(R.id.onlineStatus)
                    .setBackground(ContextCompat.getDrawable(getContext(), statusIcon));

            if (unseenCounter > 0 && !sender.equals(mAuth.getUid())) {
                msgCounter.setText(String.valueOf(unseenCounter));
                msgCounter.setVisibility(View.VISIBLE);
            }

            if (lastMessage.getFileType() != null) {
                lastMsg.setText(lastMessage.getFileType());
                lastMsg.setTypeface(null, Typeface.ITALIC);
            } else {
                lastMsg.setText(lastMessage.getContent());
            }

            int messageColor = R.color.unreadMessage;
            if (!sender.equals(mAuth.getUid())) {
                messageColor = R.color.outcomeMessage;
            } else if (lastMessage.isRead()) {
                messageColor = R.color.borderColor;
            }
            lastMsg.setVisibility(View.VISIBLE);
            lastMsg.setTextColor(ContextCompat.getColor(ma, messageColor));

            Date dateOfSend = new Date(lastMessage.getTimestamp());
            boolean isToday = dateOfSend.getDay() != new Date().getDay();
            String format = isToday ? "HH:mm:ss" : "dd MMM";
            DateFormat dateFormat = (new SimpleDateFormat(format));
            tvLastMsgTime.setText(dateFormat.format(dateOfSend));

            // Прекратить пересечивание
            if(filteredDialogList.get(position).getSpeaker().getAvatarUrl() != null) {
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
                        .load(filteredDialogList.get(position).getSpeaker().getAvatarUrl())
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.avatar))
                        .into(avatar);
            }
        }
        return convertView;
    }

    @Override
    public int getCount() {
        return filteredDialogList.size();
    }

    @NonNull
    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults result = new FilterResults();
                ArrayList<Dialog> filteredList  = new ArrayList<>();
                constraint = constraint.toString().toLowerCase();
                if( constraint.length() > 0 && constraint.toString().substring(0,1).equals("@")) {
                    constraint = constraint.subSequence(1, constraint.length());
                    for(int i = 0; i < dialogs.size(); i++) {
                        if((dialogs.get(i).getSpeaker().getNickname()).toLowerCase()
                                .contains(constraint.toString())) {
                            filteredList.add(dialogs.get(i));
                        }
                    }
                } else {
                    for(int i = 0; i < dialogs.size(); i++) {
                        if((dialogs.get(i).getSpeaker().getLogin()).toLowerCase()
                                .contains(constraint.toString())) {
                            filteredList.add(dialogs.get(i));
                        }
                    }
                }

                result.count = filteredList.size();
                result.values = filteredList;
                return result;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredDialogList = (ArrayList<Dialog>)results.values;
                if (filteredDialogList.size() > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }
}
