package com.example.wifi_bluetooth;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.io.File;

import static android.content.Context.MODE_PRIVATE;

public class SecondFragment extends Fragment {
    final int OPEN_DIR=131;

    BluetoothService service;
    boolean bound = false;
    String key;


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.button_second).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });

        view.findViewById(R.id.buttomSubmit).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                EditText simpleEditText = view.findViewById(R.id.fileKey);
                key = simpleEditText.getText().toString();
                if(key=="" || key==null) Toast.makeText(getActivity(), "Please enter key.", Toast.LENGTH_SHORT).show();
                else if(PermissionUtility.checkIOPermission(getActivity())) {

                    Intent chooserIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    try {
                        startActivityForResult(Intent.createChooser(chooserIntent,"Select a directory to save"),OPEN_DIR);

                    } catch (android.content.ActivityNotFoundException e) {
                        Toast.makeText(getActivity(), "Please install a File Manager.", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==OPEN_DIR) {
            if(resultCode==getActivity().RESULT_OK) {
                Uri uri = data.getData();
                String uriString = uri.toString();
                File myFile = new File(uriString);
                String path = myFile.getAbsolutePath();

                Intent bindBluetooth = new Intent(getActivity(),BluetoothService.class);
                getActivity().bindService(bindBluetooth, connection, Context.BIND_IMPORTANT);
                service.quest(key,path);
            }
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder serviceBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothService.BluetoothBinder binder = (BluetoothService.BluetoothBinder) serviceBinder;
            service = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = new Intent(getActivity(), BluetoothService.class);
        getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onPause() {
        super.onPause();
        // unbind
        getActivity().unbindService(connection);
        bound = false;
    }

}