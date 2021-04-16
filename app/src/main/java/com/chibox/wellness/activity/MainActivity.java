package com.chibox.wellness.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.chibox.wellness.R;
import com.chibox.wellness.adapter.ProgramAdapter;
import com.chibox.wellness.model.Program;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    static final String TAG = "ChiBoxActivity";

    ListView lvProgramList;
    ProgramAdapter mProgramAdapter;
    ArrayList<Program> mPrograms = new ArrayList<Program>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lvProgramList = findViewById(R.id.lvProgramList);
        lvProgramList.setOnItemClickListener(this);
        mProgramAdapter = new ProgramAdapter(this, mPrograms);
        lvProgramList.setAdapter(mProgramAdapter);
        getPrograms();
        ((ImageButton) findViewById(R.id.btnRefresh)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mProgramAdapter != null) {
                    mProgramAdapter.clear();
                    getPrograms();
                }
            }
        });
    }

    private void getPrograms() {
        if (Build.MANUFACTURER.equals("FiiO")) {
            Toast.makeText(this, "Get Programs in Fiio", Toast.LENGTH_SHORT).show();
            Log.e("ChiBox", "--------------- Get Programs Fiio -----------");
            for (String sdPath : new String[]{"/mnt/internal_sd/Music/ChiBox", "/mnt/external_sd1/Music/ChiBox", "/mnt/external_sd2/Music/ChiBox"}) {
                loadPrograms(new File(sdPath));
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.e("ChiBox", "--------------- Get Programs No Fiio -----------");
                Toast.makeText(this, "Get Programs in LOLLIPOP and above", Toast.LENGTH_SHORT).show();
                for (File f : getExternalMediaDirs()) {
                    String sdPath2 = f.toString();
                    loadPrograms(new File(sdPath2.substring(0, sdPath2.length() - 33) + "/Music/ChiBox"));
                }
            } else {
                Toast.makeText(this, "Get Programs in LOLLIPOP and below", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadPrograms(File path) {
        if (!path.exists()) {
            path.mkdirs();
        }
        if (path.list() == null) return;
        ArrayList<String> files = new ArrayList<String>(Arrays.asList(Objects.requireNonNull(path.list())));
        Collections.sort(files, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        if (!files.isEmpty()) {
            for (String file : files) {
                if (file.endsWith(".meta")) {
                    try {
                        mProgramAdapter.add(readMetaFile(path, file));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            mProgramAdapter.notifyDataSetChanged();
        }
    }

    private Program readMetaFile(File file, String filename) throws IOException {
        File metaFile = new File(file, filename);
        Program program = new Program();
        JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(metaFile), "UTF-8"));
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            char c = 65535;
            switch (name.hashCode()) {
                case -909675094:
                    if (name.equals("sample")) {
                        c = 4;
                        break;
                    }
                    break;
                case -734768633:
                    if (name.equals("filename")) {
                        c = 0;
                        break;
                    }
                    break;
                case -622722335:
                    if (name.equals("modality")) {
                        c = 3;
                        break;
                    }
                    break;
                case 112800:
                    if (name.equals("res")) {
                        c = 5;
                        break;
                    }
                    break;
                case 110371416:
                    if (name.equals("title")) {
                        c = 1;
                        break;
                    }
                    break;
                case 1613773252:
                    if (name.equals("encrypted")) {
                        c = 2;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    File fullPath = new File(file, reader.nextString());
                    program.filename = fullPath.toString();
                    program.fileSize = fullPath.length();
                    break;
                case 1:
                    program.title = reader.nextString();
                    break;
                case 2:
                    program.encrypted = reader.nextBoolean();
                    break;
                case 3:
                    program.modality = reader.nextInt();
                    break;
                case 4:
                    program.sampleRate = reader.nextInt();
                    break;
                case 5:
                    program.resolution = reader.nextInt();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        reader.close();
        program.calculate();
        return  program;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent playerIntent = new Intent(this, WavPlayer.class);
        playerIntent.putExtra("program", mPrograms.get(position));
        startActivity(playerIntent);
    }
}