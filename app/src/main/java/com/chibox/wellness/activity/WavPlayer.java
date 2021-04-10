package com.chibox.wellness.activity;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.chibox.wellness.R;
import com.chibox.wellness.model.Program;
import com.chibox.wellness.receiver.ScreenOffAdminReceiver;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class WavPlayer extends AppCompatActivity {

    Program mProgram;
    DevicePolicyManager deviceManger;
    ComponentName compName;
    AudioManager mAudioManager;
    AudioPlayerActivity.SettingsContentObserver mSettingsContentObserver;
    TextView mProgressText;
    SeekBar mProgressBar;
    ImageButton mPlayButton;
    AudioTrack mAudioTrack;
    InputStream mInputSream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_wav_player);
        deviceManger = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        compName = new ComponentName(this, ScreenOffAdminReceiver.class);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mAudioManager.setStreamVolume(1, mAudioManager.getStreamMaxVolume(1), 0);
        mAudioManager.setStreamVolume(3, mAudioManager.getStreamMaxVolume(3), 0);

        Intent intent = getIntent();
        mProgram = (Program) intent.getParcelableExtra("program");

        ((TextView) findViewById(R.id.title)).setText(mProgram.title);
        ((TextView) findViewById(R.id.treatmentDuration)).setText("" + mProgram.duration);
        mProgressText = (TextView) findViewById(R.id.treatmentProgress);
        mProgressText.setText("0");
        mProgressBar = (SeekBar) findViewById(R.id.progressBar);
        mProgressBar.setMax((int) (mProgram.fileSize / ((long) mProgram.inputBufferSize)));
        mProgressBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mPlayButton = (ImageButton) findViewById(R.id.buttonPlay);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAudio();
            }
        });
        ((ImageButton) findViewById(R.id.btnBack)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        prepareAudio();
    }

    public void prepareAudio() {
        InputStream in = null;
        OutputStream out = null;
        try {
            File outputFile = new File(getExternalCacheDir(), mProgram.title + "-" + mProgram.fileSize + ".wav");
            if (!outputFile.exists()) {
                in = new FileInputStream(mProgram.filename);
                out = new FileOutputStream(outputFile);
                byte[] buffer = new byte[1024];
                int read;
                boolean isFirstSegment = true;
                while ((read = in.read(buffer)) != -1) {
                    if (isFirstSegment) {
                        byte[] slice = Arrays.copyOfRange(buffer, 99, read);
                        out.write(slice, 0, slice.length);
                    } else {
                        out.write(buffer, 0, read);
                    }
                    isFirstSegment = false;
                }
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            }
        }  catch (FileNotFoundException fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        }
        catch (Exception e) {
            Log.e("tag", e.getMessage());
        }
    }

    public void updateProgress(long total) {
        mProgressBar.setProgress((int) (total / ((long) mProgram.inputBufferSize)));
        mProgressText.setText("" + (int) (total / ((long) ((mProgram.sampleRate * 2) * mProgram.resolution))));
    }

    public void changeViewImageResource(final ImageView imageView, @DrawableRes final int resId) {
        imageView.setRotation(0.0f);
        imageView.animate().rotationBy(360.0f).setDuration(400).setInterpolator(new OvershootInterpolator()).start();
        imageView.postDelayed(new Runnable() {
            public void run() {
                imageView.setImageResource(resId);
            }
        }, 200);
    }

    public void initialize() {
        try {
            mInputSream = new FileInputStream(new File(mProgram.filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mProgram.sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioTrack.getMinBufferSize(mProgram.sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
        mAudioTrack.play();
    }

    public void playAudio() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                initialize();
                long total = 0;
                byte j = 85;
                boolean completed = false;
                int bufferSize = 512;
                int i = 0;
                byte[] s = new byte[bufferSize];
                DataInputStream dis = new DataInputStream(mInputSream);
                if (mAudioTrack == null) return;
                while (true) {
                    try {
                        i = dis.read(s, 0, bufferSize);
                    } catch (IOException ex) {
                        i = -1;
                    }
                    if (i == -1) break;
                    total += (long) i;
                    updateProgress(total);
                    if (mProgram.encrypted) {
                        byte k = j;
                        for (int j2 = 0; j2 < s.length; j2++) {
                            byte p = s[j2];
                            s[j2] = (byte) (s[j2] ^ (k ^ 68));
                            k = p;
                        }
                        j = k;
                    }
                    mAudioTrack.write(s, 0, i);
                }
                try {
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                completed = true;
                mAudioTrack.stop();
                mAudioTrack.flush();
                mAudioTrack.release();
            }
        });

    }

}