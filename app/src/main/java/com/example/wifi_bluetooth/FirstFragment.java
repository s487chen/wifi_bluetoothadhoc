package com.example.wifi_bluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputFilter;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    public int network = -1;
    private int nid = -1; // 0: new adhoc. 1: connect


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

                    // specify a network
                    startOrConnectDialog();
                    if(nid==0 && network ==-1) return;

                    getActivity().startForegroundService(wifiIntent);
                    bluetoothIntent.putExtra("network",network);
                    if(nid==0) bluetoothIntent.setAction("master");
                    else bluetoothIntent.setAction("slave");
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

    private void startOrConnectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Ad Hoc Network").setSingleChoiceItems(new String[] {"Initiate New Network","Connect to Existing Network"},
                1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        nid = which;

                    }
                }).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(nid ==0) {
                    // new ad hoc
                    EnterNumberDialog(true);
                } else if(nid==1) {
                    // connect
                    chooseNetworkDialog();
                }
            }

        });
        builder.show();
    }

    private void chooseNetworkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Ad Hoc Network");
        builder.setMessage("Specify a Network");
        // Add the buttons
        builder.setPositiveButton("Any Network", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                Snackbar.make(getView(),"Connect to Any Network",BaseTransientBottomBar.LENGTH_SHORT);

            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.cancel();
            }
        }).setNeutralButton("Specify One", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                EnterNumberDialog(false);
            }
        });
        builder.show();
    }

    private void EnterNumberDialog(boolean isNewAdhoc) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Ad Hoc Network").setMessage("Enter a Key");
        final EditText input = new EditText(getActivity());
        input.setHint("1 digit only");
        input.setFilters(new InputFilter[] {
                // Maximum 2 characters.
                new InputFilter.LengthFilter(1),
                // Digits only.
                DigitsKeyListener.getInstance(),  // Not strictly needed, IMHO.
        });
        input.setKeyListener(DigitsKeyListener.getInstance());

        builder.setView(input).setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                if(input.getText().toString().length()>0) {
                    network = Integer.valueOf(input.getText().toString());
                    if(isNewAdhoc) {
                        Snackbar.make(getView(),"Create New Network "+network,BaseTransientBottomBar.LENGTH_SHORT);
                    }
                    else Snackbar.make(getView(),"Connect to Network "+network,BaseTransientBottomBar.LENGTH_SHORT);
                }
                else{
                    if(isNewAdhoc) {
                        ((Switch)getView().findViewById(R.id.switchConnect)).setChecked(false);
                        Snackbar.make(getView(),"Fail to Initiate Network",BaseTransientBottomBar.LENGTH_SHORT);
                    }
                    Snackbar.make(getView(),"Connect to Any Network",BaseTransientBottomBar.LENGTH_SHORT);
                }

            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                if(isNewAdhoc) ((Switch)getView().findViewById(R.id.switchConnect)).setChecked(false);
                else Snackbar.make(getView(),"Connect to Any Network",BaseTransientBottomBar.LENGTH_SHORT);
                dialog.cancel();
            }
        });
        builder.show();
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
            if(!getActivity().bindService(intent1, connection1, 0)) ;

            Intent intent2 = new Intent(getActivity(), WifiService.class);
            getActivity().bindService(intent2, connection2, 0);
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

    private void unBindService() {
        try {
            getActivity().unbindService(connection1);
        } catch(NullPointerException e) {}

        bluetoothBound = false;
        try {
            getActivity().unbindService(connection2);
        } catch(NullPointerException e) {}
        wifiBound = false;

    }


    @Override
    public void onPause() {
        super.onPause();
        // unbind
        unBindService();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_ENABLE_BT && resultCode==getActivity().RESULT_OK) {
            ;
        }
    }
}