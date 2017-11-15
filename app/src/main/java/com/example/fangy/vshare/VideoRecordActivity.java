package com.example.fangy.vshare;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by fangy on 2017/10/29.
 */


public class VideoRecordActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "VideoRecordActivity";
    public static final int CONTROL_CODE = 1;

    //UI
    private ImageView recordControl;
   // private ImageView pauseRecord;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Chronometer recordTime;

    private boolean isRecording;
   // private boolean isPause;
   // private long pauseTime;

    private Camera camera;
    private MediaRecorder mediaRecorder;

    private File mVideoFile;
    private String currentVideoFilePath;
    private String saveVideoPath = "";

    private Handler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler {
        private final WeakReference<VideoRecordActivity> mActivity;

        public MyHandler(VideoRecordActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            System.out.println(msg);
            if (mActivity.get() == null) {
                return;
            }
            switch (msg.what) {
                case CONTROL_CODE:

                    mActivity.get().recordControl.setEnabled(true);
                    break;
            }
        }
    }

    private MediaRecorder.OnErrorListener OnErrorListener = new MediaRecorder.OnErrorListener() {
        @Override
        public void onError(MediaRecorder mr, int what, int extra) {
            try {
                if (mr != null) {
                    mr.reset();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        initView();
    }

    private void initView() {
        surfaceView = (SurfaceView) findViewById(R.id.record_surfaceView);
        recordControl = (ImageView) findViewById(R.id.record_control);
        recordTime = (Chronometer) findViewById(R.id.record_time);
       // pauseRecord = (ImageView) findViewById(R.id.record_pause);
        recordControl.setOnClickListener((View.OnClickListener) this);
        //pauseRecord.setOnClickListener((View.OnClickListener) this);
        //pauseRecord.setEnabled(false);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(surfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setFixedSize(320, 280);
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(mCallBack);
    }

    private SurfaceHolder.Callback mCallBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            initCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (surfaceHolder.getSurface() == null) {
                return;
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            stopCamera();
        }
    };

    /**
     * Initialize camera
     */
    private void initCamera() {
        if (camera != null) {
            stopCamera();
        }

        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        if (camera == null) {
            Toast.makeText(this, "Cannot get camera", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            camera.setPreviewDisplay(surfaceHolder);
            setCameraParams();
            camera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * Set camera parameters
     */
    private void setCameraParams() {
        if (camera != null) {
            Camera.Parameters params = camera.getParameters();
            if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                params.set("orientation", "portrait");
                camera.setDisplayOrientation(90);
            } else {
                params.set("orientation", "landscape");
                camera.setDisplayOrientation(0);
            }

            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            params.setRecordingHint(true);
            if (params.isVideoStabilizationSupported())
                params.setVideoStabilization(true);
            camera.setParameters(params);
        }
    }

    /**
     * release camera resource
     */
    private void stopCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    /**
     * start record video
     */
    public void startRecord() {
        initCamera();
        camera.unlock();
        setConfigRecord();
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        isRecording = true;
        recordTime.setBase(SystemClock.elapsedRealtime());
        recordTime.start();
    }

    /**
     * Stop recording video
     */
    public void stopRecord() {
        if (isRecording && mediaRecorder != null) {
            mediaRecorder.setOnErrorListener(null);
            mediaRecorder.setPreviewDisplay(null);
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;

            recordTime.stop();
            isRecording = false;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.record_control:
                if (!isRecording) {
                    startRecord();
                    recordControl.setImageResource(R.drawable.recordvideo_stop);
                    recordControl.setEnabled(false);
                    mHandler.sendEmptyMessageDelayed(CONTROL_CODE, 1000);
                } else {
                    recordControl.setImageResource(R.drawable.recordvideo_start);
                    stopRecord();
                    camera.lock();
                    stopCamera();
                    recordTime.stop();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (!(saveVideoPath.equals(""))) {
                                String[] str = new String[]{saveVideoPath, currentVideoFilePath};
                                File reName = new File(saveVideoPath);
                                File f = new File(getSDPath(VideoRecordActivity.this) + "append.mp4");
                                f.renameTo(reName);
                                if (reName.exists()) {
                                    f.delete();
                                    new File(currentVideoFilePath).delete();
                                }
                            }
                            Intent intent = new Intent(VideoRecordActivity.this, UploadActivity.class);
                            Bundle bundle = new Bundle();
                            if (saveVideoPath.equals("")) {
                                bundle.putString("videoPath", currentVideoFilePath);
                            } else {
                                bundle.putString("videoPath", saveVideoPath);
                            }
                            intent.putExtras(bundle);
                            Log.e(TAG, "Start uploadActivity");
                            startActivity(intent);
                            finish();
                        }
                    }).start();
                }

                break;
        }
    }

    /**
     * Create file path
     */
    private boolean createRecordDir() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(this, "Please check you SDCard！", Toast.LENGTH_SHORT).show();
            return false;
        }

        File sampleDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Record");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        String recordName = "VID" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".mp4";
        mVideoFile = new File(sampleDir, recordName);
        currentVideoFilePath = mVideoFile.getAbsolutePath();
        return true;
    }

    public static String getSDPath(Context context) {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
        } else if (!sdCardExist) {
            Toast.makeText(context, "No SDCard", Toast.LENGTH_SHORT).show();
        }
        File eis = new File(sdDir.toString() + "/VShare/");
        try {
            if (!eis.exists()) {
                eis.mkdir();
            }
        } catch (Exception e) {

        }
        return sdDir.toString() + "/VShare/";
    }

    /**
     * Set parameters of mediaRecorder
     */
    private void setConfigRecord() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.reset();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setOnErrorListener(OnErrorListener);
        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        mediaRecorder.setAudioEncodingBitRate(44100);
        mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);

        mediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
        mediaRecorder.setOrientationHint(90);
        mediaRecorder.setVideoSize(1280, 720);

        currentVideoFilePath = getSDPath(getApplicationContext()) + getVideoName();
        mediaRecorder.setOutputFile(currentVideoFilePath);
    }

    private String getVideoName() {
        return "VID" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".mp4";
    }

    private void setToResult() {
        Intent intent = new Intent();
        intent.putExtra("videoPath", currentVideoFilePath);
        setResult(RESULT_OK, intent);
        finish();
    }

}


