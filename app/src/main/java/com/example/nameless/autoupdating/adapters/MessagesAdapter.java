package com.example.nameless.autoupdating.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.common.ChatActions;
import com.example.nameless.autoupdating.common.MediaFileUtils.MediaFileDownloader;
import com.example.nameless.autoupdating.common.MediaFileUtils.RunningAudio;
import com.example.nameless.autoupdating.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nameless on 07.04.18.
 */

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    private Context ma;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private ArrayList<Message> messages;

//    public static Picasso mPicasso;

    public MessagesAdapter(Context ma, ArrayList<Message> messages, DatabaseReference myRef) {
//        mPicasso = Picasso.with(ma);
        RunningAudio.initInstance(); // init Audio player
        this.ma = ma;
        this.messages = messages;
        this.myRef = myRef;
        mAuth = FirebaseAuth.getInstance();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        LayoutInflater li = (LayoutInflater)ma.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
//        View v  = li.inflate(R.layout.message_item, parent, false);

        View v = LayoutInflater.from(ma).inflate(R.layout.message_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setMessageLayout(position);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private View view;

        public ViewHolder(View itemView) {
            super(itemView);
            this.view = itemView;
        }

        public void setMessageLayout(int position) {

            LinearLayout layout = view.findViewById(R.id.content);
            RelativeLayout.LayoutParams layoutParams =
                    (RelativeLayout.LayoutParams) layout.getLayoutParams();
            RelativeLayout.LayoutParams timeLayoutParams =
                    (RelativeLayout.LayoutParams) view.findViewById(R.id.tvDate).getLayoutParams();

            Message currentMessage = messages.get(position);
            final String fileUrl = currentMessage.getFileUrl();

            if((currentMessage.getWho()).equals(mAuth.getUid())) {
                layout.setBackgroundResource(R.drawable.message_background_out);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                timeLayoutParams.addRule(RelativeLayout.LEFT_OF, R.id.content);
            } else {
                layout.setBackgroundResource(R.drawable.message_background_in);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                timeLayoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.content);
            }

            if(fileUrl != null) {
                MediaFileDownloader downloadTask =
                        new MediaFileDownloader(ma, layout, currentMessage);
                downloadTask.downloadFileByUrl();
            }

            parseMessageContent(currentMessage, view);

            DateFormat dateFormat = (new SimpleDateFormat("HH:mm:ss \n dd MMM"));
            ((TextView)view.findViewById(R.id.tvDate)).setText(dateFormat
                    .format(currentMessage.getDateOfSend()));
        }
    }

    private void parseMessageContent(final Message message, View convertView) {
        String msg = message.getContent();

        if (message.getFileType() != null && (message.getFileType()).equals("Url")) {

            Pattern urlPattern = Pattern.compile(
                    "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d" +
                            ":#@%/;$()~_?\\+-=\\\\\\.&]*)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

            Matcher matcher = urlPattern.matcher(msg);
            while (matcher.find()) {
                int matchStart = matcher.start(0);
                int matchEnd = matcher.end(0);
                final String findUrl = msg.substring(matchStart, matchEnd);

                ViewGroup.LayoutParams lparam = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                final TextView urlPart = new TextView(ma);
                urlPart.setText(findUrl);
                urlPart.setLayoutParams(lparam);
                urlPart.setTextColor(ma.getResources().getColor(R.color.lincIonicColor));
                urlPart.setTextSize(16);
                urlPart.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
                float dpi = ma.getResources().getDisplayMetrics().density;
                urlPart.setMaxWidth((int)(240 * dpi));

                urlPart.setOnClickListener(v -> {
                    Intent i = new Intent();
                    i.setAction(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(findUrl));
                    ma.startActivity(i);
                });

      /*          urlPart.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                            urlPart.setBackgroundColor(Color.rgb(30, 144, 255));
                        return false;
                    }
                });*/


                ((LinearLayout)convertView.findViewById(R.id.content)).addView(urlPart);
                msg = msg.replace(findUrl, "");
            }
        }

        (convertView.findViewById(R.id.msgItem)).setOnClickListener(v -> showPopupMenu(v, message));

        TextView tvContent = convertView.findViewById(R.id.tvContent);
        if(msg.length() > 0) {
            tvContent.setText(msg);
        } else {
            tvContent.setVisibility(View.GONE);
        }

        if(!message.isRead()) {
            if(message.getTo().equals(mAuth.getUid())) {
                String uid = message.getUid();
                message.setRead(true);
                message.setUid(null);
                myRef.child(uid).setValue(message);
                message.setUid(uid);
//                notifyDataSetChanged();
            } else {
                (convertView.findViewById(R.id.content))
                        .setBackgroundResource(R.drawable.message_background_not_readed);
            }
        }
    }

    private void showPopupMenu(final View view, final Message msg) {

        LayoutInflater layoutInflater
                = (LayoutInflater)ma.getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.message_context, null);

        final PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        popupView.setOnClickListener(v -> popupWindow.dismiss());
        Button btnCopy = popupView.findViewById(R.id.btnCopy);
        Button btnEdit= popupView.findViewById(R.id.btnEdit);
        Button btnDelete= popupView.findViewById(R.id.btnDelete);

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager)ma.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("buff", ((TextView)view.findViewById(R.id.tvContent)).getText());
            clipboard.setPrimaryClip(clip);
            popupWindow.dismiss();
        });
        btnEdit.setOnClickListener(v -> {
//                FirebaseStorage storage = FirebaseStorage.getInstance();
//                StorageReference edRef = storage.getReferenceFromUrl(msg.getFileUrl());
            ChatActions iChatActions = (ChatActions) ma;
            iChatActions.onEdit(msg);
            popupWindow.dismiss();
        });
        btnDelete.setOnClickListener(v -> {
            if(msg.getFileUrl() != null ) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference delRef = storage.getReferenceFromUrl(msg.getFileUrl());
                delRef.delete();
                //Мб можно еще и с устройства удалить, но лень
            }
            myRef.child(msg.getUid()).removeValue();
            messages.remove(msg);
            notifyDataSetChanged();
            popupWindow.dismiss();
        });
    }
}
