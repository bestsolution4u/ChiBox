package com.chibox.wellness.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.chibox.wellness.R;
import com.chibox.wellness.model.Program;
import com.chibox.wellness.util.TimeUtils;

import java.util.ArrayList;

public class ProgramAdapter extends BaseAdapter {

    private LayoutInflater inflater = null;
    private ArrayList<Program> programs;

    public ProgramAdapter(Context context, ArrayList<Program> programs) {
        this.programs = programs;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return programs.size();
    }

    @Override
    public Object getItem(int position) {
        return programs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Program program = programs.get(position);
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.program_line, null);
        }
        ((TextView) view.findViewById(R.id.tvProgramName)).setText(program.title);
        ((TextView) view.findViewById(R.id.tvProgramDuration)).setText(TimeUtils.formatDuration(program.duration));
        return view;
    }

    public void add(Program object) {
        this.programs.add(object);
    }

    public void clear() {
        programs.clear();
    }
}
