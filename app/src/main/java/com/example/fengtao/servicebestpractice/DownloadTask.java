package com.example.fengtao.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;

import com.example.fengtao.servicebestpractice.Interface.DownloadListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String,Integer,Integer> {
    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;
    
    private DownloadListener listener;
    private boolean isCanceled = false;
    private boolean isPause = false;
    private int lastProgress;
    
    public DownloadTask(DownloadListener listener){
        this.listener = listener;
    }
    
    @Override
    protected Integer doInBackground(String... params){
        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;
        try {
            long downloadLength = 0;
            String downloadUrl = params[0];
            //xx.substring(0,2)表示取第一个和第二个字符（0,1,2表示第一、二、三个字符，含头不含尾的原则就只包含第一、二个字符），返回一个新的字符串（只包含指定的第一和第二个字符）；
            //xx.substring(2)表示去掉前两个字符，返回一个新的字符串（只包含去掉前两个字符后剩下的字符串）
            //lastIndexOf(string str)返回指定字符串最后出现的位置
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory + fileName);
            if(file.exists()){
                downloadLength = file.length();
            }
            long contentlength = getContentLength(downloadUrl);
            if(contentlength == 0){
                return TYPE_FAILED;
            }else if(contentlength == downloadLength){
                return TYPE_SUCCESS;
            }
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RANGE","bytes=" + downloadLength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if(response != null){
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file,"rw");
                savedFile.seek(downloadLength);
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while((len=is.read(b))!= -1){
                    if(isCanceled){
                        return TYPE_CANCELED;
                    }else if(isPause){
                        return TYPE_PAUSED;
                    }else {
                        total += len;
                        savedFile.write(b,0,len);
                        int progress = (int)((total + downloadLength)*100/contentlength);
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try {
                if(is != null){
                    is.close();
                }
                if(savedFile != null){
                    savedFile.close();
                }
                if(isCanceled && file != null){
                    file.delete();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }
    
    @Override
    protected void onProgressUpdate(Integer... value){
        int progress = value[0];
        if(progress > lastProgress){
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }
    
    @Override
    protected void onPostExecute(Integer status){
        switch(status){
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
            default:
                break;
        }
    }
    
    public void pauseDownload(){
        isPause = true;
    }
    
    public void cancelDownload(){
        isCanceled = true;
    }
    
    private long getContentLength(String downloadUrl) throws IOException{
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if(response != null && response.isSuccessful()){
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }
}
