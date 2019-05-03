package com.example.jeffemuveyan.uploadvideo.UtilClasses;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.example.jeffemuveyan.uploadvideo.Interfaces.OnProgressOperation;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.jaiselrahman.filepicker.model.MediaFile;
import androidx.annotation.NonNull;


public class FileUploader {

    private StorageReference mStorageRef;
    private StorageTask storageTask;

    private Uri videoUri;
    private MediaFile videoFile;
    private Context c;

    public int fileSize;
    public int bytesUploaded;

    private OnProgressOperation onProgressOperation;

    public FileUploader(Context c, Uri videoUri, MediaFile videoFile, OnProgressOperation onProgressOperation){

        this.videoUri = videoUri;
        this.videoFile = videoFile;
        this.c = c;
        this.onProgressOperation = onProgressOperation;

        mStorageRef = FirebaseStorage.getInstance().getReference().child("sermons");

    }


    public void uploadVideo(){

        StorageReference videoRef = mStorageRef.child(videoFile.getName());
        //Note: Any name you give to the videoRef child above is the name firebase will use to
        //store your file o. So, logically, you should make the child name same as your video file name.

        storageTask = videoRef.putFile(videoUri) //we nees that storageTask so we can pause/resume.
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        Uri downloadUrl = taskSnapshot.getUploadSessionUri();

                        //Toast.makeText(c, downloadUrl.toString(), Toast.LENGTH_LONG).show();
                        onProgressOperation.onFinished("Upload complete!", "Video has been successfully uploaded");

                    }
                }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(c, "Paused", Toast.LENGTH_SHORT).show();
                    }

                }).addOnCanceledListener(new OnCanceledListener() {
                    @Override
                    public void onCanceled() {
                        Toast.makeText(c, "canceld oooooooooo", Toast.LENGTH_SHORT).show();
                    }
                })

                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads

                        onProgressOperation.onFinished("Upload failed!", "Video upload failed. Try again");

                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                        fileSize = (int)taskSnapshot.getTotalByteCount();
                        bytesUploaded = (int)taskSnapshot.getBytesTransferred();

                        onProgressOperation.onProgress(fileSize, bytesUploaded);
                    }
                });

    }


    public void pauseUpload(){
        storageTask.pause();
    }

    public void resumeUpload() {
        storageTask.resume();
        Toast.makeText(c, "Resuming upload", Toast.LENGTH_SHORT).show();
    }

    public void cancelUpload() {
        storageTask.cancel();
        onProgressOperation.onFinished("Upload canceled!", "Video upload has been canceled");
    }


    public int getFileSize() {
        return fileSize;
    }

    public int getBytesUploaded() {
        return bytesUploaded;
    }

}
