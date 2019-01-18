package yqb.com.example.audiorecorddemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.Manifest.permission.RECORD_AUDIO;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener {

    private Button startBt;
    private Button stopBt;
    private boolean isDownCenter = false;
    // 是否在录制
    private boolean isRecording = false;
    // pcm文件
    private File file;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 申请一个（或多个）权限，并提供用于回调返回的获取码（用户定义）
            requestPermissions(REQUIRED_PERMISSIONS, 100);
        }

        startBt = (Button) findViewById(R.id.startRecord);
        stopBt = (Button) findViewById(R.id.stopRecord);

        startBt.setOnTouchListener(this);
        stopBt.setOnClickListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(!isDownCenter){
                    doRecord();
                }
                isDownCenter = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDownCenter = false;
                stopRecord();
                break;
        }
        return false;
    }

    private void doRecord() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                StartRecord();
            }
        });
        thread.start();
        startBt.setText(getResources().getString(R.string.recording));
    }

    // 开始录音
    public void StartRecord() {
        // 16K采集率
        int frequency = 16000;
        // 格式
        int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        // 16Bit
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        // 生成PCM文件
        file = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/" + createRecordFileName(this));
        // 如果存在，就先删除再创建
        if (file.exists())
            file.delete();
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create " + file.toString());
        }

        try {
            //输出流
            FileOutputStream os = new FileOutputStream(file);
            int bufferSize = AudioRecord.getMinBufferSize(frequency,
                    channelConfiguration, audioEncoding);
            AudioRecord audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC, frequency,
                    channelConfiguration, audioEncoding, bufferSize);

            byte data[] = new byte[bufferSize];
            audioRecord.startRecording();
            isRecording = true;
            while (isRecording) {
                int bufferReadResult = audioRecord.read(data, 0, bufferSize);
                os.write(data);
            }
            audioRecord.stop();
            os.close();
        } catch (Throwable t) {
            Log.e("TAG", "Failure of recording");
        }
    }

    private void stopRecord() {
        isRecording = false;
        startBt.setText(getResources().getString(R.string.record));
    }

    public static String createRecordFileName(Context mContext) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date) + ".pcm";
    }

    @Override
    public void onClick(View view) {
        PlayRecord();
    }

    // 播放文件
    public void PlayRecord() {
        if (file == null) {
            return;
        }
        // 读取文件
        int musicLength = (int) (file.length() / 2);
        short[] music = new short[musicLength];
        try {
            InputStream is = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);
            int i = 0;
            while (dis.available() > 0) {
                music[i] = dis.readShort();
                i++;
            }
            dis.close();
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    16000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, musicLength * 2,
                    AudioTrack.MODE_STREAM);
            audioTrack.play();
            audioTrack.write(music, 0, musicLength);
            audioTrack.stop();
        } catch (Throwable t) {
            Log.e("TAG", "Play failure");
        }
    }
}
