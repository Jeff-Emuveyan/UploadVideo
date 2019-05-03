package com.example.jeffemuveyan.uploadvideo.Interfaces;

public interface OnProgressOperation {

    public void onFinished(String status, String message);
    public void onProgress(int fileSize, int bytesUploaded);
}
