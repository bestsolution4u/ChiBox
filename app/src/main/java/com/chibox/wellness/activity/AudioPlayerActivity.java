package com.chibox.wellness.activity;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
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
import com.chibox.wellness.interfaces.IChiBoxPlayer;
import com.chibox.wellness.model.Program;
import com.chibox.wellness.receiver.ScreenOffAdminReceiver;
import com.chibox.wellness.service.ChiBoxService;

public class AudioPlayerActivity extends AppCompatActivity {

    Program mProgram;
    DevicePolicyManager deviceManger;
    ComponentName compName;
    AudioManager mAudioManager;
    SettingsContentObserver mSettingsContentObserver;
    TextView mProgressText;
    SeekBar mProgressBar;
    ImageButton mPlayButton;
    boolean mShouldUnbind;
    ChiBoxService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_audio_player);

        deviceManger = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        compName = new ComponentName(this, ScreenOffAdminReceiver.class);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mAudioManager.setStreamVolume(1, mAudioManager.getStreamMaxVolume(1), 0);
        mAudioManager.setStreamVolume(3, mAudioManager.getStreamMaxVolume(3), 0);
        mSettingsContentObserver = new SettingsContentObserver(new Handler());
        getApplicationContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, mSettingsContentObserver);

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
                if (!mService.mIsPlaying) {
                    changeViewImageResource((ImageButton) v, R.drawable.pause_button);
                    mService.play();
                    boolean active = deviceManger.isAdminActive(compName);
                    if (active) {
                        deviceManger.lockNow();
                    }
                } else {
                    changeViewImageResource((ImageButton) v, R.drawable.play_button);
                    mService.pause();
                }
            }
        });
        ((ImageButton) findViewById(R.id.btnBack)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mShouldUnbind = false;
        if (bindService(new Intent(this, ChiBoxService.class), mConnection, BIND_AUTO_CREATE)) {
            mShouldUnbind = true;
        }
    }

    @Override
    protected void onDestroy() {
        if (mShouldUnbind) {
            Log.e("AudioPlayerActivity", "UnbindService");
            mService.setCallbacks(null);
            unbindService(mConnection);
            mShouldUnbind = false;
        }
        super.onDestroy();
    }

    public void updateProgress(long total) {
        mProgressBar.setProgress((int) (total / ((long) mProgram.inputBufferSize)));
        mProgressText.setText("" + (int) (total / ((long) (mProgram.sampleRate * mProgram.modality * mProgram.resolution))));
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

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((ChiBoxService.LocalBinder) service).getService();
            if (mService.mProgram == null || !mProgram.filename.equals(mService.mProgram.filename)) {
                mService.setProgram(mProgram);
            }
            if (!mService.mIsPlaying) {
                mPlayButton.setImageResource(R.drawable.play_button);
            } else {
                mPlayButton.setImageResource(R.drawable.pause_button);
            }
            mService.setCallbacks(mCallback);
            updateProgress(mService.mTotal);
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    public IChiBoxPlayer mCallback = new IChiBoxPlayer.Stub() {
        public void setProgress(final long total) {
            runOnUiThread(new Runnable() {
                public void run() {
                    updateProgress(total);
                }
            });
        }

        public void resetPlayButton() {
            runOnUiThread(new Runnable() {
                public void run() {
                    Log.e("AudioPlayerActivity", "=============onResetPlayerButton=========");
                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock  wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.ON_AFTER_RELEASE, "com.chibox.wellness.activity::WakeLock");
                    wakeLock.acquire(10);
                    wakeLock.release();
                    changeViewImageResource(mPlayButton, R.drawable.play_button);
                    mProgressBar.setProgress(0);
                    mProgressText.setText("0");
                }
            });
        }
    };

    public class SettingsContentObserver extends ContentObserver {
        public SettingsContentObserver(Handler paramHandler) {
            super(paramHandler);
        }

        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        public void onChange(boolean paramBoolean) {
            super.onChange(paramBoolean);
            mAudioManager.setStreamVolume(1, mAudioManager.getStreamMaxVolume(1), 0);
            mAudioManager.setStreamVolume(3, mAudioManager.getStreamMaxVolume(3), 0);
        }
    }
}