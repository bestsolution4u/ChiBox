package com.chibox.wellness.model;

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;

public class Program implements Parcelable {

    public int duration;
    public boolean encrypted;
    public long fileSize;
    public String filename;
    public int format;
    public int inputBufferSize;
    public int modality;
    public int resolution;
    public int sampleRate;
    public String title;

    public Program() {
        this.filename = null;
        this.title = null;
        this.encrypted = false;
        this.modality = 0;
        this.sampleRate = 0;
        this.resolution = 0;
        this.fileSize = 0;
        this.duration = 0;
        this.inputBufferSize = 0;
        this.format = 0;
    }

    protected Program(Parcel in) {
        String[] data = new String[6];
        in.readStringArray(data);
        this.filename = data[0];
        this.title = data[1];
        this.encrypted = Boolean.parseBoolean(data[2]);
        this.modality = Integer.parseInt(data[3]);
        this.sampleRate = Integer.parseInt(data[4]);
        this.resolution = Integer.parseInt(data[5]);
        this.fileSize = new File(this.filename).length();
        calculate();
    }

    public void calculate() {
        switch (this.resolution) {
            case 1:
                this.format = AudioFormat.ENCODING_PCM_8BIT;
                break;
            case 2:
                this.format = AudioFormat.ENCODING_PCM_16BIT;
                break;
            default:
                this.format = AudioFormat.ENCODING_PCM_FLOAT;
                break;
        }
        Log.e("aaa", "" + this.format);
        this.duration = (int) (this.fileSize / ((long) (this.sampleRate * this.modality * this.resolution)));
        this.inputBufferSize = AudioTrack.getMinBufferSize(this.sampleRate, this.modality == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO, this.format);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{this.filename, this.title, String.valueOf(this.encrypted), String.valueOf(this.modality), String.valueOf(this.sampleRate), String.valueOf(this.resolution)});
    }

    @Override
    public int describeContents() {
        return 0;
    }

    static String toString(int duration2) {
        int hr = duration2 / 3600;
        int duration3 = duration2 % 3600;
        int min = duration3 / 60;
        int sec = duration3 % 60;
        if (hr == 0) {
            return String.format("%d:%02d", new Object[]{Integer.valueOf(min), Integer.valueOf(sec)});
        }
        return String.format("%d:%02d:%02d", new Object[]{Integer.valueOf(hr), Integer.valueOf(min), Integer.valueOf(sec)});
    }

    public static final Creator<Program> CREATOR = new Creator<Program>() {
        @Override
        public Program createFromParcel(Parcel in) {
            return new Program(in);
        }

        @Override
        public Program[] newArray(int size) {
            return new Program[size];
        }
    };
}
