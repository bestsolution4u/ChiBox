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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
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
import android.widget.Toast;

import com.chibox.wellness.R;
import com.chibox.wellness.model.Program;
import com.chibox.wellness.receiver.ScreenOffAdminReceiver;
import com.chibox.wellness.util.TimeUtils;

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
import java.util.Timer;
import java.util.TimerTask;

public class WavPlayer extends AppCompatActivity {

    Program mProgram;
    DevicePolicyManager deviceManger;
    ComponentName compName;
    TextView tvPlayerPosition, tvDuration;
    SeekBar mProgressBar;
    ImageButton mPlayButton;
    private MediaPlayer mediaPlayer;
    int mDuration = 0;
    boolean prepared = false;

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

        Intent intent = getIntent();
        mProgram = (Program) intent.getParcelableExtra("program");

        ((TextView) findViewById(R.id.title)).setText(mProgram.title);
        tvDuration = findViewById(R.id.tvDuration);
        tvPlayerPosition = (TextView) findViewById(R.id.tvPlayerPosition);
        mProgressBar = (SeekBar) findViewById(R.id.progressBar);
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
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        changeViewImageResource((ImageButton) v, R.drawable.play_button);
                        mediaPlayer.pause();
                    } else {
                        changeViewImageResource((ImageButton) v, R.drawable.pause_button);
                        mediaPlayer.start();
                        boolean active = deviceManger.isAdminActive(compName);
                        if (active) {
                            deviceManger.lockNow();
                        }
                    }
                }
            }
        });
        ((ImageButton) findViewById(R.id.btnBack)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateProgress();
                    }
                });
            }
        },0, 1000);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.setLooping(false);
                prepared = true;
                mDuration = mediaPlayer.getDuration();
                tvDuration.setText(TimeUtils.formatDuration(((int)(mDuration / 1000))));
                tvPlayerPosition.setText("00:00");
                mProgressBar.setMax(mDuration);
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (!prepared) {
                    Toast.makeText(WavPlayer.this, "Wait while loading audio...", Toast.LENGTH_SHORT).show();
                    return;
                }
                PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock  wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE, "com.chibox.wellness.activity::WakeLock");
                wakeLock.acquire(10);
                wakeLock.release();
                changeViewImageResource(mPlayButton, R.drawable.play_button);
                mProgressBar.setProgress(0);
                tvPlayerPosition.setText("00:00");
            }
        });
        prepareAudio();
    }

    public void prepareAudio() {
        InputStream in = null;
        OutputStream out = null;
        try {
            File outputFile = new File(getExternalCacheDir(), mProgram.title + "-" + mProgram.fileSize + ".wav");
            if (outputFile.exists()) {
                outputFile.delete();
            }
            in = new FileInputStream(mProgram.filename);
            out = new FileOutputStream(outputFile);
            byte[] buffer = new byte[1024 * 1024];
            int read;
            byte j = 85;
            while ((read = in.read(buffer)) != -1) {
                if (mProgram.encrypted) {
                    byte k = j;
                    for (int j2 = 0; j2 < buffer.length; j2++) {
                        byte p = buffer[j2];
                        buffer[j2] = (byte) (buffer[j2] ^ (k ^ 68));
                        k = p;
                    }
                    j = k;
                }
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            mediaPlayer.setDataSource(outputFile.getPath());
            mediaPlayer.prepareAsync();
        } catch (Exception fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        }
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

    public void updateProgress() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            int position = mediaPlayer.getCurrentPosition();
            tvPlayerPosition.setText(TimeUtils.formatDuration(((int)(position / 1000))));
            mProgressBar.setProgress(position);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}