package com.example.fengtao.servicebestpractice.Interface;

public interface DownloadListener { 
    void onProgress(int progress);
    
    void onSuccess();
    
    void onFailed();
    
    void onPaused();
    
    void onCanceled();
}