package com.example.fangy.vshare;

/**
 * Created by fangy on 2017/10/30.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.widget.Toast.LENGTH_LONG;


public class UploadActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ProgressBar progressBar;
    private String filePath = null;
    private TextView txtPercentage;
   // private ImageView imgPreview;
    private VideoView vidPreview;
    private Button btnUpload;
    long totalSize = 0;
    private String workPath;
    private String videoName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        txtPercentage = (TextView) findViewById(R.id.txtPercentage);
        btnUpload = (Button) findViewById(R.id.btnUpload);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        vidPreview = (VideoView) findViewById(R.id.videoPreview);

        // Receiving the data from previous activity
        Intent i = getIntent();
        // video path that is captured in previous activity
       filePath = i.getExtras().getString("videoPath");
      //  filePath = "/storage/emulated/0/VShare/ElephantsDream.mp4";

        if (filePath != null) {
            // Displaying video on the screen
            previewVideo();
        } else {
            Toast.makeText(getApplicationContext(),
                    "Sorry, file path is missing!", LENGTH_LONG).show();
        }

        btnUpload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // uploading the file to server
                new UploadFileToServer().execute();
            }
        });

    }

    /**
     * Displaying captured video on the screen
     * */
    private void previewVideo() {
            vidPreview.setVisibility(View.VISIBLE);
            vidPreview.setVideoPath(filePath);
            // start playing
            vidPreview.start();
    }

    /**
     * Uploading the file to server
     * */
    private class UploadFileToServer extends AsyncTask<Void, Integer, String> {
        @Override
        protected void onPreExecute() {
            // setting progress bar to zero
            progressBar.setProgress(0);
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // Making progress bar visible
            progressBar.setVisibility(View.VISIBLE);
            // updating progress bar value
            progressBar.setProgress(progress[0]);
            // updating percentage value
            txtPercentage.setText(String.valueOf(progress[0]) + "%");
        }

        @Override
        protected String doInBackground(Void... params) {
            Log.e(TAG, "start clipVideo");
            int num = 0;
            String response;
            String uploadSuccess = "Upload success";
            String filePathToUpload;
            num =  clipVideo(filePath);

            for (int i = 1; i <= num; i++) {
                filePathToUpload = workPath + "/" + videoName + "_" + i + ".mp4";
                response = uploadFile(filePathToUpload);
            }

            return "Upload success";
        }

        private String uploadFile(String filePathUpload) {
            long requestTime = System.currentTimeMillis();
            long responseTime = 0;

            String result = null;
            String boundary = "****";
            String twoHyphens = "--";
            String end = "\r\n";

            try {
                URL url = new URL(Config.FILE_UPLOAD_URL);
                Log.e(TAG, Config.FILE_UPLOAD_URL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                //httpURLConnection.setChunkedStreamingMode(12 * 1024 * 1024);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setUseCaches(false);
                httpURLConnection.setRequestMethod("POST");

                httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
                httpURLConnection.setRequestProperty("Charset", "UTF-8");
                httpURLConnection.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=" + boundary);

                Log.e(TAG, "1");
                Log.e(TAG, filePath);

                DataOutputStream dos = new DataOutputStream(httpURLConnection.getOutputStream());
                Log.e(TAG, "filePathToUplaod = "+ filePathUpload);

                dos.writeBytes(twoHyphens + boundary + end);
                dos.writeBytes("Content-Disposition: form-data; name=\"fileToUpload\"; filename=\""
                             + filePathUpload.substring(filePathUpload.indexOf("/")) + "\"" + end);
                dos.writeBytes("Content-Type: video/mp4" + end);
                dos.writeBytes(end);

                FileInputStream fis = new FileInputStream(filePathUpload);

                byte[] buffer = new byte[1024];
                int count = 0;
                // Reed file
                while ((count = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, count);
                }

                dos.writeBytes(end);
                fis.close();

                dos.writeBytes(end);
                dos.writeBytes(end);
                // dos.writeBytes(twoHyphens + boundary + twoHyphens + end);

                dos.writeBytes(twoHyphens + boundary + twoHyphens + end);
                dos.flush();

                int res = httpURLConnection.getResponseCode();
                responseTime = System.currentTimeMillis();
                Log.e(TAG, "response code = " + res);
                Log.e(TAG, "resonse time = " + (responseTime - requestTime));

                InputStream is = httpURLConnection.getInputStream();
                StringBuffer sb = new StringBuffer();
                int ss;
                while ((ss = is.read() )!= -1) {
                    sb.append((char) ss);
                }
                result = sb.toString();
                Log.e(TAG, "Response = " + result);

                // Toast.makeText(this, result, LENGTH_LONG).show();
                dos.close();
                is.close();

            } catch (IOException e) {
                e.printStackTrace();
                return result;
            }
            Log.e(TAG, result);
            return "Upload failed";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.e(TAG, "Response from server: " + result);

            // showing the server response in an alert dialog
            showAlert(result);
            super.onPostExecute(result);
        }
    }

    /**
     * Method to show alert dialog
     * */
    private void showAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setTitle("Response from Servers")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // do nothing
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Clip Video, 3 seconds each
     * @param path
     */
    private int clipVideo(String path) {
        File file = new File(path);
        workPath = file.getParent();
        String fileName = file.getName();
        videoName = fileName.substring(0, fileName.lastIndexOf("."));


        Log.e(TAG, "workPath = " + workPath);
        Log.e(TAG, "videoName = " + videoName);

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        Log.e(TAG, "duration " + duration);
        int totalTime = Integer.valueOf(duration);
        int numberOfFile = (int) Math.ceil((double) totalTime/(double) 3000);
        Log.e(TAG, "numberOfFile " + numberOfFile);
        VideoClip.setFilePath(path);
        VideoClip.setWorkingPath(workPath);

        for (int i = 1; i <= numberOfFile; i++)
        {
            VideoClip.setOutName(videoName + "_" + i + ".mp4");
            VideoClip.setStartTime(3000 * (i - 1) + 1000);
            if (i == 1) {
                VideoClip.setStartTime(0);
            }
            VideoClip.setEndTime(3000 * i  );
            if (i == numberOfFile) {
                VideoClip.setEndTime(3000 * i +1000 );
            }
            VideoClip.clip();
        }
        Log.e(TAG, path );
        return numberOfFile;
    }

}
