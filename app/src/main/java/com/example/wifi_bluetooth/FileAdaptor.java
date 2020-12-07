package com.example.wifi_bluetooth;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class FileAdaptor extends ArrayAdapter<FileEntry> {
    public FileAdaptor(@NonNull Context context, @NonNull ArrayList<FileEntry> objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        FileEntry fileEntry = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_file, parent, false);

        }

        TextView fName = (TextView) convertView.findViewById(R.id.fName);
        TextView fHash = (TextView) convertView.findViewById(R.id.fHash);
        TextView fPath = (TextView) convertView.findViewById(R.id.fPath);

        fName.setText(fileEntry.fname);
        fHash.setText(fileEntry.fhash);
        fPath.setText(fileEntry.fpath);

        return convertView;
    }
}
