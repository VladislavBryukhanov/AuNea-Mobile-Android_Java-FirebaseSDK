package com.example.nameless.autoupdating;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
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
    private ArrayList<Message> messages;
    private ArrayList<Message> filteredMessageList;
    private Map<String, Bitmap> imageCollection;
    private Map<String, Uri> uriForIntentCollection;

    public MessagesAdapter(Context ma, ArrayList<Message> messages) {
        super(ma, 0, messages);
        this.ma = ma;
        this.messages = messages;
        this.filteredMessageList = messages;
        imageCollection = new HashMap<>();
        uriForIntentCollection = new HashMap<>();
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

                /*if(filteredMessageList.get(position).getFileUrl() != null) {
                    layoutParams = (RelativeLayout.LayoutParams) convertView.findViewById(
                                    R.id.ivImage).getLayoutParams();
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                }*/
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

    /*            if(filteredMessageList.get(position).getFileUrl() != null) {
                    layoutParams = (RelativeLayout.LayoutParams) convertView.findViewById(
                            R.id.ivImage).getLayoutParams();
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                }*/
            }

            if(filteredMessageList.get(position).getFileUrl() != null) {

                (convertView.findViewById(R.id.ivImage)).setVisibility(View.VISIBLE);


//                ((ImageView)convertView.findViewById(R.id.ivImage))
//                        .setImageBitmap(BitmapFactory.decodeResource(getContext().getResources(),
//                                R.drawable.loading));

                ((ImageView)convertView.findViewById(R.id.ivImage))
                        .setImageBitmap(downloadFileByUrl(filteredMessageList
                                .get(position).getFileUrl(),
                                (ImageView)convertView.findViewById(R.id.ivImage)));

                ((ImageView)convertView.findViewById(R.id.ivImage)).setAdjustViewBounds(true);

            }

            parseMessageContent(filteredMessageList.get(position).getContent(), convertView);

//            ((TextView)convertView.findViewById(R.id.tvContent))
//                .setText(filteredMessageList.get(position).getContent());

            DateFormat dateFormat = (new SimpleDateFormat("HH:mm:ss \n dd MMM"));
            ((TextView)convertView.findViewById(R.id.tvDate)).setText(dateFormat
                    .format(filteredMessageList.get(position).getDateOfSend()));
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

    public Bitmap downloadFileByUrl(final String url, final ImageView iv) {
        if (imageCollection.get(url) != null) {
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
/*                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(uriForIntentCollection.get(url), "image*//*");
                    ma.startActivity(intent);*/
                    Intent intent = new Intent(ma, ImageViewer.class);
                    intent.putExtra("bitmap", uriForIntentCollection.get(url));
                    ma.startActivity(intent);
                }
            });
            return imageCollection.get(url);
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference fileReference = storage.getReferenceFromUrl(url);

//        File file = null;
//        try {
    //.createTempFile("images", "jpg");

//            file = File.createTempFile(fileReference.getName(), null,
//                    getContext().getCacheDir());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        fileReference.getFile(file);

        File path = new File(Environment.getExternalStorageDirectory()
                + "/AUMessanger/");
        if(!path.exists()) {
            path.mkdir();
        }

        File imgFile = new File(path, fileReference.getName());
        if (!imgFile.exists()) {
            fileReference.getFile(imgFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            iv.setImageBitmap(downloadFileByUrl(url, iv));
                }
            });
        }

        //getting screen size & and calculating optimal scale for image
        Bitmap image = BitmapFactory.decodeFile(imgFile.getPath());
        if (image == null) {
            return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.loading);
        }

        WindowManager wm = (WindowManager) getContext().getSystemService(
                Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);


        size.x *= 0.5;
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        double scale = (double) imageWidth / size.x ;
        if(imageWidth > size.x) {
            imageWidth = size.x;
            imageHeight /= scale;
        }
        imageCollection.put(url, Bitmap.createScaledBitmap(image, imageWidth,
                imageHeight, true));

        uriForIntentCollection.put(url, Uri.parse(imgFile.getPath()));
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent();
//                intent.setAction(Intent.ACTION_VIEW);
//                intent.setDataAndType(uriForIntentCollection.get(url), "image/*");
//                ma.startActivity(intent);
                Intent intent = new Intent(ma, ImageViewer.class);
                intent.putExtra("bitmap", uriForIntentCollection.get(url));
                ma.startActivity(intent);
            }
        });
        return imageCollection.get(url);

//        String path = getContext().getCacheDir().getAbsolutePath() + file.getName();
//        return BitmapFactory.decodeFile(imgFile.getPath());
    }


    public void parseMessageContent(String message, View convertView) {
        String msg = message;
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
            urlPart.setTextColor(Color.BLUE);

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

            ((LinearLayout)convertView.findViewById(R.id.content))
                    .addView(urlPart);
            message = message.replace(findUrl, "");
        }

        TextView tvContent = (TextView)convertView.findViewById(R.id.tvContent);
        if(message.length() > 0) {
            tvContent.setText(message);
        } else {
            tvContent.setVisibility(View.GONE);
        }
    }
}
