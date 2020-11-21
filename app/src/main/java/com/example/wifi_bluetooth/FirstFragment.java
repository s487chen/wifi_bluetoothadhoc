package com.example.wifi_bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import static android.content.Context.MODE_PRIVATE;
import static android.util.Log.d;


public class FirstFragment extends Fragment {

    private final int REQUEST_ENABLE_BT = 120;
    BluetoothService bluetoothService;
    WifiService wifiService;
    final Intent bluetoothIntent = new Intent(getActivity().getApplicationContext(), BluetoothService.class);
    final Intent wifiIntent = new Intent(getActivity().getApplicationContext(), WifiService.class);
    boolean bluetoothBound = false;
    boolean wifiBound = false;

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
                // request download
                if(bluetoothBound && wifiBound)
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
                else Snackbar.make(view,"Please connect to Ad-hoc network first.", BaseTransientBottomBar.LENGTH_SHORT);
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

                if(isChecked) {
                    //Checks if the permission is Enabled or not...
                    if(!PermissionUtility.checkWifiPermissions(getActivity(),true)) {
                        if(!PermissionUtility.checkWifiPermissions(getActivity(),false)) {
                            buttonView.setChecked(false);
                            preferences.edit().putBoolean("is_online", false).apply();
                            return;
                        }
                    }

                    // Initializes Bluetooth adapter.
                    final BluetoothManager bluetoothManager =
                            (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
                    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

                    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                        buttonView.setChecked(false);
                        preferences.edit().putBoolean("is_online", false).apply();
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        return;
                    }

                    getActivity().startForegroundService(wifiIntent);
                    getActivity().startForegroundService(bluetoothIntent);

                    preferences.edit().putBoolean("is_online", true).apply();

                } else {
                    //Stop service
                    getActivity().stopService(bluetoothIntent);
                    getActivity().stopService(wifiIntent);
                    buttonView.setChecked(false);
                    preferences.edit().putBoolean("is_online", false).apply();
                }
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences preferences = getActivity().getSharedPreferences("IS_ONLINE", MODE_PRIVATE);
        boolean isCheck = preferences.getBoolean("isOnline",false);
        ((Switch)getActivity().findViewById(R.id.switchConnect)).setChecked(isCheck);

        if(isCheck) {
            // bind to service
            Intent intent1 = new Intent(getActivity(), BluetoothService.class);
            getActivity().bindService(intent1, connection1, Context.BIND_AUTO_CREATE);

            Intent intent2 = new Intent(getActivity(), WifiService.class);
            getActivity().bindService(intent2, connection2, Context.BIND_AUTO_CREATE);
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection1 = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder serviceBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothService.BluetoothBinder binder = (BluetoothService.BluetoothBinder) serviceBinder;
            bluetoothService = binder.getService();
            bluetoothBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bluetoothBound = false;
        }
    };

    private ServiceConnection connection2 = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder serviceBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            WifiService.WifiBinder binder = (WifiService.WifiBinder) serviceBinder;
            wifiService = binder.getService();
            wifiBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            wifiBound = false;
        }
    };



    @Override
    public void onPause() {
        super.onPause();
        // unbind
        getActivity().unbindService(connection1);
        bluetoothBound = false;
        getActivity().unbindService(connection2);
        wifiBound = false;
    }
}