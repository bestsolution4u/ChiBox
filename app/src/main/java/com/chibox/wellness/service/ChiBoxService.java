package com.chibox.wellness.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.chibox.wellness.interfaces.IChiBoxPlayer;
import com.chibox.wellness.model.Program;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

public class ChiBoxService extends Service {

    static final String TAG = "ChiBoxService";

    public AudioTrack mAudioTrack = null;
    private final IBinder mBinder = new LocalBinder();
    public IChiBoxPlayer mCallbacks = null;
    InputStream mInputSream = null;
    public boolean mIsPlaying = false;
    Semaphore mSemaphore = null;
    Thread mThread = null;
    public long mTotal = 0;
    public Program mProgram = null;

    private Runnable mRunnable = new Runnable() {
        public void run() {
            mTotal = 0;
            byte j = 85;
            boolean completed = false;
            int bufferSize = 256;
            int i = 0;
            byte[] s = new byte[bufferSize];
            Log.e("Service", "Initialize Dis");
            DataInputStream dis = new DataInputStream(mInputSream);
            while (true) {
                try {
                    mSemaphore.acquire();
                    mSemaphore.release();
                    Log.e("Semaphore", "Released");
                    try {
                        i = dis.read(s, 0, bufferSize);
                    } catch (IOException ex) {
                        i = -1;
                    }
                    if (i == -1) break;
                    mTotal += (long) i;
                    if (mCallbacks != null) {
                        try {
                            mCallbacks.setProgress(mTotal);
                        } catch (RemoteException e2) {
                        }
                    }
                    if (mProgram.encrypted) {
                        byte k = j;
                        for (int j2 = 0; j2 < s.length; j2++) {
                            byte p = s[j2];
                            s[j2] = (byte) (s[j2] ^ (k ^ 68));
                            k = p;
                        }
                        j = k;
                    }
                    if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                        mAudioTrack.write(s, 0, i);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                dis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            completed = true;
            if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                mAudioTrack.stop();
            }
            mAudioTrack.release();
            mIsPlaying = false;
            if (completed) {
                initialize();
                if (mCallbacks != null) {
                    try {
                        mCallbacks.resetPlayButton();
                    } catch (RemoteException e4) {
                    }
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mSemaphore = new Semaphore(1, true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setProgram(Program t) {
        mProgram = t;
        if (mProgram != null) {
            initialize();
        }
    }

    public void initialize() {
        try {
            if (mInputSream != null) {
                mInputSream.close();
                mInputSream = null;
            }
            mTotal = 0;
            mInputSream = new FileInputStream(new File(mProgram.filename));
            mAudioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    mProgram.sampleRate,
                    mProgram.modality == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                    mProgram.format,
                    mProgram.inputBufferSize,
                    AudioTrack.MODE_STREAM);
            mAudioTrack.play();
            pause();
            mThread = new Thread(mRunnable);
            mThread.start();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void play() {
        mIsPlaying = true;
        if (mSemaphore.availablePermits() == 0) {
            mSemaphore.release();
        }
    }

    public void pause() {
        mIsPlaying = false;
        if (mSemaphore.availablePermits() == 1) {
            mSemaphore.acquireUninterruptibly();
        }
    }

    public void stop() {
        if (mCallbacks != null) {
            try {
                mCallbacks.setProgress(0);
            } catch (RemoteException e) {
            }
        }
        this.mThread.interrupt();
        try {
            this.mThread.join();
        } catch (InterruptedException e2) {
        }
        mTotal = 0;
        pause();
    }

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        /* access modifiers changed from: package-private */
        public ChiBoxService getService() {
            return ChiBoxService.this;
        }
    }

    public void setCallbacks(IChiBoxPlayer callbacks) {
        mCallbacks = callbacks;
    }
}