package com.example.jeffemuveyan.uploadvideo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.provider.MediaStore.Video.Thumbnails;
import android.widget.Toast;

import com.example.jeffemuveyan.uploadvideo.Services.UploadService;
import com.example.jeffemuveyan.uploadvideo.UtilClasses.Constants;
import com.jaiselrahman.filepicker.activity.FilePickerActivity;
import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.MediaFile;

import java.io.File;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    //Visit for https://developer.android.com/guide/components/services#java
    //https://www.truiton.com/2014/10/android-foreground-service-example/
    //https://www.truiton.com/2014/09/android-service-broadcastreceiver-example/
    //https://developer.android.com/guide/components/broadcasts#java

    //Don't forget to request ForegroundService Permission in AndroidManifest

    private static final int FILE_REQUEST_CODE = 33;
    ImageView imageView;
    Button chooseButton, uploadButton, pauseButton, resumeButton, cancelButton;
    TextView titleTextView, percentageTextView;
    ProgressBar progressBar;

    private Uri videoUri;
    private MediaFile videoFile;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView)findViewById(R.id.imageView);
        chooseButton = (Button)findViewById(R.id.button);
        uploadButton = (Button)findViewById(R.id.button2);
        pauseButton = (Button)findViewById(R.id.button3);
        cancelButton = (Button)findViewById(R.id.button4);
        resumeButton = (Button)findViewById(R.id.button5);
        titleTextView = (TextView) findViewById(R.id.textView);
        percentageTextView = (TextView) findViewById(R.id.textView2);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        //disable some views
        hideViews(true);

        createNotificationChannel();


        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                chooseVideo();
            }
        });


        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                hideViews(false);

                //Now, we start the service to upload the video
                //( Don't forget to declare the service class in Manifest file.)
                Intent intent = new Intent(MainActivity.this, UploadService.class);
                intent.setAction(Constants.START_SERVICE);//its not too important but we will use it in the service.
                intent.putExtra("videoUri", videoUri);
                intent.putExtra("videoFile", videoFile);

                //for latest Android Phones (Android 8+)
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    startForegroundService(intent);
                }else{
                    startService(intent);
                }

            }
        });


        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pauseIntent = new Intent(MainActivity.this, UploadService.class);
                pauseIntent.setAction(Constants.PAUSE);

                //for latest Android Phones (Android 8+)
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    startForegroundService(pauseIntent);
                }else{
                    startService(pauseIntent);
                }
            }
        });


        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // resumeUpload();
            }
        });


        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cancelIntent = new Intent(MainActivity.this, UploadService.class);
                cancelIntent.setAction(Constants.CANCEL);

                //for latest Android Phones (Android 8+)
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    startForegroundService(cancelIntent);
                }else{
                    startService(cancelIntent);
                }
            }
        });


        //create a IntentFilter to filter all possible Intent that may be sent from the Service to MainActivity
        IntentFilter mIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);//important for new phones
        mIntentFilter.addAction(Constants.PROGRESS_STATE);

        //Now register the BroadCastReceiver
        registerReceiver(mReceiver, mIntentFilter); //Don't forget to stop the receiver in onDestroy.
    }



    private void chooseVideo(){
        Intent intent = new Intent(this, FilePickerActivity.class);
        intent.putExtra(FilePickerActivity.CONFIGS, new Configurations.Builder()
                .setCheckPermission(true)
                .setShowImages(false)
                .setShowAudios(false)
                .setShowVideos(true)
                .enableImageCapture(true)
                .setMaxSelection(1)
                .setSkipZeroSizeFiles(true)
                .build());
        startActivityForResult(intent, FILE_REQUEST_CODE);

        /*Intent intent = new Intent();
        intent.setType("**");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select file to upload"), REQUEST_FILE_SELECT);*/
    }


    public Bitmap retrieveThumbnailFromVideo(String video_Path){

        Bitmap bitmap;

        bitmap = ThumbnailUtils.createVideoThumbnail(video_Path, Thumbnails.MINI_KIND);

        return bitmap;
    }


    private void hideViews(Boolean aBoolean){

        if(aBoolean){//if true, hide the views
            uploadButton.setEnabled(false);
            pauseButton.setVisibility(View.GONE);
            resumeButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            percentageTextView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        }else{
            uploadButton.setEnabled(true);
            pauseButton.setVisibility(View.VISIBLE);
            resumeButton.setVisibility(View.VISIBLE);
            cancelButton.setVisibility(View.VISIBLE);
            percentageTextView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }
    }


    private void createNotificationChannel(){ //If you don't call this method, you notifications will only show on older versions of android phones.

        final String CHANNEL_ID = "com.example.jeffemuveyan.uploadvideo.ANDROID";//Our Notification in MyFireBaseService will use this same id to get the channel.
        final String CHANNEL_NAME = "UploadVideo channel";

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription("Uploading video...");
            channel.enableVibration(true);
            channel.enableLights(true);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            //Toast.makeText(SplashActivity.this, "New Phone", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        try {

            switch (requestCode) {

                case 33:
                    ArrayList<MediaFile> files = data.getParcelableArrayListExtra(FilePickerActivity.MEDIA_FILES);

                    videoFile = files.get(0);//get the video file

                    //now display the video thumbnail
                    imageView.setImageBitmap(retrieveThumbnailFromVideo(videoFile.getPath()));

                    //and the video file name
                    titleTextView.setText(videoFile.getName());

                    videoUri = Uri.fromFile(new File(videoFile.getPath()));

                    //enable the upload button:
                    uploadButton.setEnabled(true);

                    break;
            }

            //NOTE: ALL THESES EXCEPTIONS ARE CAUSED BY THE LIBRARY WE ARE USING TO SELECT AUDIO. OUR OWN CODE IF 100% EXCELLENT
        }catch(IndexOutOfBoundsException e){// this exception occurs when the user pressing 'Done' when no song was selected
            Toast.makeText(MainActivity.this, "No video selected", Toast.LENGTH_SHORT).show();
        }catch(NullPointerException e){// this exception occurs when the user tries the go back from the audio picker
            //don't do anything
            Toast.makeText(MainActivity.this, "No video selected", Toast.LENGTH_SHORT).show();
        }catch(Exception e){// this exception occurs when the user tries the go back from the audio picker
            //don't do anything
            Toast.makeText(MainActivity.this, "No video selected", Toast.LENGTH_SHORT).show();
        }
    }


    //AnonymousInner Class
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Constants.PROGRESS_STATE)) {

                int fileSize = Integer.parseInt(intent.getStringExtra("fileSize"));
                int bytesUploaded = Integer.parseInt(intent.getStringExtra("bytesUploaded"));

                progressBar.setMax(fileSize);
                progressBar.setProgress(bytesUploaded);

                titleTextView.setText(String.valueOf(fileSize)+"\n/"+String.valueOf(bytesUploaded));
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
