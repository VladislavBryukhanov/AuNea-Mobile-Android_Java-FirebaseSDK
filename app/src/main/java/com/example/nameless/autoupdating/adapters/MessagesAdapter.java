package com.example.nameless.autoupdating.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nameless.autoupdating.activities.AudioTrackUI;
import com.example.nameless.autoupdating.activities.Chat;
import com.example.nameless.autoupdating.activities.UserList;
import com.example.nameless.autoupdating.asyncTasks.DownloadMediaFIle;
import com.example.nameless.autoupdating.R;
import com.example.nameless.autoupdating.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nameless on 07.04.18.
 */

public class MessagesAdapter extends ArrayAdapter<Message>  implements Filterable{

    private Context ma;
    private EditText etMessage;

    private Point screenSize;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private ArrayList<Message> messages;
    private ArrayList<Message> filteredMessageList;

    public static Pair<String, LinearLayout> runningAudio;
    public static MediaPlayer mediaPlayer;
    public static Handler trackDurationHandler;
    public static LruCache<String, Bitmap> mMemoryCache;

//    public static Picasso mPicasso;

    public MessagesAdapter(Context ma, EditText etMessage, ArrayList<Message> messages, DatabaseReference myRef) {
        super(ma, 0, messages);
//        mPicasso = Picasso.with(ma);
        this.etMessage = etMessage;
        this.ma = ma;
        this.messages = messages;
        this.filteredMessageList = messages;
        mMemoryCache = new LruCache<String, Bitmap>(40) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return 1;
            }
        };

        this.myRef = myRef;
        mAuth = FirebaseAuth.getInstance();
        mediaPlayer = new MediaPlayer();
        trackDurationHandler = new Handler();

        WindowManager wm = (WindowManager) ma.getSystemService(
        Context.WINDOW_SERVICE);
        screenSize = new Point();
        wm.getDefaultDisplay().getSize(screenSize);
        screenSize.x *= 0.5;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(filteredMessageList.size() > 0) {
            LayoutInflater li = (LayoutInflater)ma.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            convertView = li.inflate(R.layout.message_item, parent, false);

            if((filteredMessageList.get(position).getWho())
                    .equals(mAuth.getUid())) {
                (convertView.findViewById(R.id.content))
                        .setBackgroundResource(R.drawable.message_background_out);

                RelativeLayout.LayoutParams layoutParams =
                        (RelativeLayout.LayoutParams) convertView.findViewById(
                                R.id.content).getLayoutParams();
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

                layoutParams = (RelativeLayout.LayoutParams) convertView.findViewById(
                                R.id.tvDate).getLayoutParams();
                layoutParams.addRule(RelativeLayout.LEFT_OF, R.id.content);
            } else {
                (convertView.findViewById(R.id.content))
                        .setBackgroundResource(R.drawable.message_background_in);

                RelativeLayout.LayoutParams layoutParams =
                        (RelativeLayout.LayoutParams) convertView.findViewById(
                                R.id.content).getLayoutParams();

                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

                layoutParams = (RelativeLayout.LayoutParams) convertView.findViewById(
                        R.id.tvDate).getLayoutParams();
                layoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.content);
            }

            final String fileUrl = filteredMessageList.get(position).getFileUrl();
            if(fileUrl != null) {

                ImageView image = convertView.findViewById(R.id.ivImage);
                ProgressBar loading = convertView.findViewById(R.id.pbLoading);
                LinearLayout audioUI = convertView.findViewById(R.id.audioUI);

                String fileMediaSides = filteredMessageList.get(position).getFileMediaSides();
                if(fileMediaSides != null) {
                    setMediaItemSize(fileMediaSides, loading, image);
                }

//                Bitmap bitmap = MessagesAdapter.mMemoryCache.get(filteredMessageList.get(position).getFileUrl());
//                if(bitmap != null) {
//                    image.setImageBitmap(bitmap);
//                    image.setVisibility(View.VISIBLE);
//                } else {
//                    loading.setVisibility(View.VISIBLE);
//                }

                if((filteredMessageList.get(position).getFileType()).equals("image")) {
                    loading.setVisibility(View.VISIBLE);
                } else if((filteredMessageList.get(position).getFileType()).equals("audio")) {
                    ImageView audioButton = audioUI.findViewById(R.id.audioButton);
                    ProgressBar audioPbLoading = audioUI.findViewById(R.id.pbLoading);
                    TextView timeDuration = audioUI.findViewById(R.id.tvTime);

                    timeDuration.setText("Loading...");
                    audioPbLoading.setVisibility(View.VISIBLE);
                    audioButton.setVisibility(View.GONE);
                    audioUI.setVisibility(View.VISIBLE);
                }


//                if((filteredMessageList.get(position).getFileType()).equals("image")) {
//                    mPicasso.load(fileUrl)
//                            .resize(200, 200)
//                            . centerInside()
//                            .into(image);
//                    loading.setVisibility(View.GONE);
//                    image.setVisibility(View.VISIBLE);
//                } else {
                    Bitmap bitmap = MessagesAdapter.mMemoryCache.get(fileUrl);
                    if(bitmap != null) {
                        loading.setVisibility(View.GONE);
                        image.setVisibility(View.VISIBLE);
                        image.setImageBitmap(bitmap);
                    }

                    DownloadMediaFIle downloadTask = new DownloadMediaFIle(
                            image, loading, audioUI, getContext(),
                            filteredMessageList.get(position).getFileType());
                    downloadTask.execute(fileUrl);
//                }


                image.setAdjustViewBounds(true);
            }

            parseMessageContent(filteredMessageList.get(position), convertView);

            DateFormat dateFormat = (new SimpleDateFormat("HH:mm:ss \n dd MMM"));
            ((TextView)convertView.findViewById(R.id.tvDate)).setText(dateFormat
                    .format(filteredMessageList.get(position).getDateOfSend()));
        }
        return convertView;
    }

    private void setMediaItemSize(String resolution, ProgressBar loading, ImageView img) {
        int imageWidth = Integer.parseInt(resolution.split("x")[0]);
        int imageHeight = Integer.parseInt(resolution.split("x")[1]);
        double scale = (double) imageWidth / screenSize.x;
        if(imageWidth > screenSize.x) {
            imageWidth = screenSize.x;
            imageHeight /= scale;
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(imageWidth, imageHeight);
        loading.setLayoutParams(params);
        img.setLayoutParams(params);
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

    public void parseMessageContent(final Message message, View convertView) {
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
                urlPart.setTextColor(Color.rgb(0, 172, 162));
                urlPart.setTextSize(16);
                urlPart.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
                float dpi = getContext().getResources().getDisplayMetrics().density;
                urlPart.setMaxWidth((int)(240 * dpi));

                urlPart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent();
                        i.setAction(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(findUrl));
                        ma.startActivity(i);
                    }
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

        (convertView.findViewById(R.id.msgItem)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v, message);
            }
        });

        TextView tvContent = convertView.findViewById(R.id.tvContent);
        if(msg.length() > 0) {
            tvContent.setText(msg);
        } else {
            tvContent.setVisibility(View.GONE);
        }

        if(!message.isRead()) {
            if(message.getTo().equals(mAuth.getUid())) {
                message.setRead(true);
                myRef.child(message.getUid()).setValue(message);
                notifyDataSetChanged();
            } else {
                (convertView.findViewById(R.id.content)).setBackgroundResource(R.drawable.message_background_not_readed);
            }
        }
    }

    public void showPopupMenu(final View view, final Message msg) {

        LayoutInflater layoutInflater
                = (LayoutInflater)ma.getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.message_context, null);

        final PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        popupView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
        Button btnCopy = popupView.findViewById(R.id.btnCopy);
        Button btnEdit= popupView.findViewById(R.id.btnEdit);
        Button btnDelete= popupView.findViewById(R.id.btnDelete);

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager)ma.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("buff", ((TextView)view.findViewById(R.id.tvContent)).getText());
                clipboard.setPrimaryClip(clip);
                popupWindow.dismiss();
            }
        });
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                FirebaseStorage storage = FirebaseStorage.getInstance();
//                StorageReference edRef = storage.getReferenceFromUrl(msg.getFileUrl());

                Chat.onEdit(msg);
                popupWindow.dismiss();
            }
        });
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });
    }
}
