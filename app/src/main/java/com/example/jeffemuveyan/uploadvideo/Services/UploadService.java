package com.example.jeffemuveyan.uploadvideo.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.example.jeffemuveyan.uploadvideo.Interfaces.OnProgressOperation;
import com.example.jeffemuveyan.uploadvideo.MainActivity;
import com.example.jeffemuveyan.uploadvideo.R;
import com.example.jeffemuveyan.uploadvideo.UtilClasses.Constants;
import com.example.jeffemuveyan.uploadvideo.UtilClasses.FileUploader;
import com.jaiselrahman.filepicker.model.MediaFile;

import java.util.Random;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class UploadService extends Service {

    FileUploader fileUploader;
    Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Lets create some pendingIntents
        Intent startMainActivityIntent = new Intent(this, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, startMainActivityIntent, 0);

        //pendingIntents to be handled by this Service itself
        Intent pauseButtonIntent = new Intent(this, UploadService.class);
        pauseButtonIntent.setAction(Constants.PAUSE);
        final PendingIntent pauseButtonPendingIntent = PendingIntent.getService(this, 0, pauseButtonIntent, 0);

        Intent resumeButtonIntent = new Intent(this, UploadService.class);
        resumeButtonIntent.setAction(Constants.RESUME);
        final PendingIntent resumeButtonPendingIntent = PendingIntent.getService(this, 0, resumeButtonIntent, 0);

        Intent cancelButtonIntent = new Intent(this, UploadService.class);
        cancelButtonIntent.setAction(Constants.CANCEL);
        final PendingIntent cancelButtonPendingIntent = PendingIntent.getService(this, 0, cancelButtonIntent, 0);


        //lets know who is trying to do something on the service:
        if(intent.getAction().equals(Constants.START_SERVICE)) {//if its the MainActivity:

            Uri videoUri = (Uri)intent.getExtras().get("videoUri");
            MediaFile videoFile = (MediaFile)intent.getExtras().get("videoFile");


            //prepare file upload
            fileUploader = new FileUploader(getApplicationContext(), videoUri, videoFile, new OnProgressOperation() {
                @Override
                public void onFinished(String status, String message) {

                    //once the task is finished, Android system will remove the foreground notification automatically.
                    //So we will now display another ordinary notification to the user:
                    showNotification(status, message);

                    stopSelf();//we stop the service. (this is the call that actually removes the Foreground notification)
                }

                @Override
                public void onProgress(final int fileSize, final int bytesUploaded) {

                    //Service runs on UI thread so any long work should be done inside a separate thread.
                    //Now, this onProgress() runs frequently and is more or less like a while loop.
                    //If we don't wrap it inside a thread, it will freeze the Service. Your notification
                    //buttons won't respond to clicks at all.
                    //Ths is why we wrap these tasks inside a handler.
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Notification builder = new NotificationCompat.Builder(UploadService.this, Constants.CHANNEL_ID)
                                    .setSmallIcon(android.R.drawable.stat_sys_upload)
                                    .setContentTitle("UploadVideo")
                                    .setContentText(String.valueOf(bytesUploaded)+" / "+String.valueOf(fileSize))
                                    .setOngoing(true)
                                    .setProgress(fileSize, bytesUploaded, false)
                                    .addAction(R.drawable.ic_pause_black_24dp, "Pause", pauseButtonPendingIntent)
                                    .addAction(R.drawable.ic_cancel_black_24dp, "Cancel", cancelButtonPendingIntent)
                                    .setContentIntent(pendingIntent).build();

                            //Make the Service run as a Foreground Service:(Note: the duty of Foreground Service in fact:
                            // startForeground() is to just display notification.
                            //It does not start the service. It only makes the service have a foreground notification
                            startForeground(101, builder); //101 is just a random number we choose
                        }
                    });

                    //Ok, lets us also send out a Broadcast
                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction(Constants.PROGRESS_STATE);
                    broadcastIntent.putExtra("fileSize", String.valueOf(fileSize));
                    broadcastIntent.putExtra("bytesUploaded", String.valueOf(bytesUploaded));
                    sendBroadcast(broadcastIntent);
                }
            });

            fileUploader.uploadVideo();

        }else if(intent.getAction().equals(Constants.PAUSE)){
            fileUploader.pauseUpload();
        }else if(intent.getAction().equals(Constants.RESUME)){
            fileUploader.resumeUpload();
        }else if(intent.getAction().equals(Constants.CANCEL)){
            fileUploader.cancelUpload();
        }


        return START_STICKY;
    }


    private void showNotification(String title, String body) {

        Intent intent = new Intent(this, MainActivity.class).putExtra("msg",body);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);


        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_file_upload_black_24dp)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(new Random().nextInt(), notificationBuilder.build());
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }
}