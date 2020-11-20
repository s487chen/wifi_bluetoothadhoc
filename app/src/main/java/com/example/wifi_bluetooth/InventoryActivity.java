package com.example.wifi_bluetooth;

import android.Manifest;
import android.app.AlertDialog;
import android.app.AppComponentFactory;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;


public class InventoryActivity extends AppCompatActivity {
    FloatingActionButton add;
    Button back;
    ListView listView;
    ArrayList<FileEntry> arrayOfFiles;
    FileAdaptor adapter;
    private ProgressDialog progress;
    final int REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 123;
    final int ADD_FILE_PATH = 226;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inventory);

        back = findViewById(R.id.goToMain);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(InventoryActivity.this, MainActivity.class));
            }
        });

        // display files
        // Construct the data source

        arrayOfFiles = new ArrayList<FileEntry>();
        if(IOUtility.isPrefExist(this)) arrayOfFiles = IOUtility.readPref(this);
        // Create the adapter to convert the array to views

        adapter = new FileAdaptor(this, arrayOfFiles);

        // Attach the adapter to a ListView
        listView = (ListView) findViewById(R.id.lvItem);
        registerForContextMenu(listView);
        listView.setAdapter(adapter);

    }

    public void addFile(View v) {
        //Checks if the permission is Enabled or not...
        if (PermissionUtility.checkIOPermission(this)) {
            Intent pickFileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pickFileIntent.setType("*/*");
            pickFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(Intent.createChooser(pickFileIntent, "Select a file to share"), ADD_FILE_PATH);
            } catch(android.content.ActivityNotFoundException e) {
                Toast.makeText(InventoryActivity.this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {

        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Select The Action");
        menu.add(0, v.getId(), 0, "Detail");
        menu.add(0, v.getId(), 1, "Delete");
        menu.add(0, v.getId(), 2, "Delete All");

    }

    private void createDetailDialog(FileEntry f) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Information");
        builder.setMessage(f.fname+"\n"+f.fpath+"\n"+f.size+" byte");
        builder.show();
    }

    private void createDeleteDialog(FileEntry f) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning");
        builder.setMessage("Are you sure to delete the entry?");
        // Add the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                adapter.remove(f);
                IOUtility.savePref(InventoryActivity.this, arrayOfFiles);
            }
        }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void createClearDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning");
        builder.setMessage("Are you sure to clear the inventory?");
        // Add the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                adapter.clear();
                IOUtility.savePref(InventoryActivity.this, arrayOfFiles);
            }
        }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.cancel();
            }
        });
        builder.show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        //  info.position will give the index of selected item
        FileEntry f = adapter.getItem(info.position);
        if(item.getTitle()=="Detail")
        {
            createDetailDialog(f);
        }
        else if(item.getTitle()=="Delete")
        {
            createDeleteDialog(f);
        }
        else if(item.getTitle()=="Delete All")
        {
            createClearDialog();
        }
        else
        {
            return false;
        }
        return true;


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(""+requestCode,"is granted.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case ADD_FILE_PATH:
                if(resultCode==RESULT_OK) {
                    Uri uri = data.getData();
                    String uriString = uri.toString();
                    File myFile = new File(uriString);
                    long size = myFile.getTotalSpace();
                    String path = myFile.getAbsolutePath();
                    String displayName = null;

                    if (uriString.startsWith("content://")) {
                        Cursor cursor = null;
                        try {
                            cursor = InventoryActivity.this.getContentResolver().query(uri, null, null, null, null);
                            if (cursor != null && cursor.moveToFirst()) {
                                displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            }
                        } finally {
                            cursor.close();
                        }
                    } else if (uriString.startsWith("file://")) {
                        displayName = myFile.getName();
                    }
                    updateListView(displayName, path, size);
                }

                break;
            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }

    }

    public void updateListView(String name, String path, long size) {
        progress=new ProgressDialog(this);
        progress.setMessage("Verifying File");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setIndeterminate(true);
        progress.setProgress(0);
        progress.show();

        String hashCode = IOUtility.getHash(path,progress,size);
        this.adapter.add(new FileEntry(name,path,hashCode,size));
        IOUtility.savePref(this, arrayOfFiles);
    }


}
