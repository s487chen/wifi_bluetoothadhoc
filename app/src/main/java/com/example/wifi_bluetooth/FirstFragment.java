package com.example.wifi_bluetooth;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import static android.content.Context.MODE_PRIVATE;
import static android.util.Log.d;


public class FirstFragment extends Fragment {

    private final int REQUEST_ENABLE_BT = 120;
    BluetoothService service;
    Intent bluetoothIntent = new Intent(getActivity(), BluetoothService.class);
    boolean bound = false;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bound)
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
                else Snackbar.make(view,"Please connect to Adhoc network first.", BaseTransientBottomBar.LENGTH_SHORT);
            }
        });

        view.findViewById(R.id.goToInventory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), InventoryActivity.class);
                startActivity(intent);
            }
        });

        ((Switch)view.findViewById(R.id.switchConnect)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                SharedPreferences preferences = getActivity().getSharedPreferences("IS_ONLINE", MODE_PRIVATE);
                preferences.edit().putBoolean("is_online", isChecked).apply();

                if(isChecked) {
                    //Checks if the permission is Enabled or not...
                    if(!Utils.checkPermissions(getActivity(),true)) {
                        if(Utils.checkPermissions(getActivity(),false)) {
                            buttonView.setChecked(false);
                            return;
                        }
                    }

                    // Initializes Bluetooth adapter.
                    final BluetoothManager bluetoothManager =
                            (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
                    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

                    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                        buttonView.setChecked(false);
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        return;
                    }

                    getActivity().startForegroundService(bluetoothIntent);
                } else {
                    //Stop service
                    getActivity().stopService(bluetoothIntent);
                    buttonView.setChecked(false);
                }
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences preferences = getActivity().getSharedPreferences("IS_ONLINE", MODE_PRIVATE);
        boolean isCheck = preferences.getBoolean("isOnline",false);
        ((Switch)getActivity().findViewById(R.id.switchConnect)).setChecked(isCheck);

        if(isCheck) {
            // bind to service
            Intent intent = new Intent(getActivity(), BluetoothService.class);
            getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);

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
    public void onStop() {
        super.onStop();
        // unbind
        getActivity().unbindService(connection);
        bound = false;
    }
}