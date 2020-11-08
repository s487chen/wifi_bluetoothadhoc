package com.example.wifi_bluetooth;

import android.Manifest;
import android.app.AppComponentFactory;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

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
    TextInputLayout addName;
    int REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 123;
    int ADD_FILE_PATH = 226;

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

        ArrayList<FileEntry> arrayOfFiles = new ArrayList<FileEntry>();

        // Create the adapter to convert the array to views

        FileAdaptor adapter = new FileAdaptor(this, arrayOfFiles);

        // Attach the adapter to a ListView

        ListView listView = (ListView) findViewById(R.id.lvItems);

        listView.setAdapter(adapter);
    /*
        add = findViewById(R.id.addFileButton);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            // add file
                //Checks if the permission is Enabled or not...
                if (ContextCompat.checkSelfPermission(InventoryActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(InventoryActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_READ_EXTERNAL_STORAGE_PERMISSION);
                } else {
                    Intent pickFileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    pickFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(pickFileIntent, ADD_FILE_PATH);
                }
                Intent pickFileIntent = new Intent(Intent.ACTION_PICK);
                pickFileIntent.getData();
                startActivity(myIntent);



                addName = findViewById(R.id.fileNameInput);
                SharedPreferences fileList = getSharedPreferences("fileList", MODE_PRIVATE);
                SharedPreferences.Editor editor = fileList.edit();
                editor.putString("fileName",addName.getEditText().toString());
                editor.apply();

                File storage = Environment.getExternalStorageDirectory();
            }
        });
*/


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //if(requestCode == )
    }
}
