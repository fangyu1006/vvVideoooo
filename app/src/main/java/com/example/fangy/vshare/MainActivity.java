package com.example.fangy.vshare;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int RECORD_VIDEO_REQUEST_CODE = 100;
    public static final int RECORD_VIDEO = 1;

    private Button btnRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRecord = (Button) findViewById(R.id.btnRecordVideo);

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
        public void onClick(View v) {
            recordVideo();
        }
    });
    }

    /**
     * start recording video
     *
     */
    public void recordVideo() {
        Intent intent = new Intent(this, VideoRecordActivity.class);
        startActivity(intent);
        //startActivityForResult(intent, RECORD_VIDEO_REQUEST_CODE);
    }

}
