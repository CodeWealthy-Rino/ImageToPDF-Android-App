package com.kumazaku.pdfcreator.versionInfo;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.TextView;

import com.kumazaku.pdfcreator.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class VersionInfoActivity extends AppCompatActivity {


    private void setUpVerinfo()
    {
        TextView verText = (TextView) findViewById(R.id.app_ver_info);

        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo("com.kumazaku.pdfcreator2", PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
        }

        verText.setText(getString(R.string.app_name) + " Version:"+packageInfo.versionName);
    }

    private void setUpLicenseInfo() {

        EditText licenseText = (EditText)findViewById(R.id.licence_text);

        licenseText.setText(licenseString());
    }

    private String licenseString() {

        String readLines = "";

        InputStream is = getResources().openRawResource(R.raw.licenseinfo);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String readLine = null;

        try {
            while ((readLine = br.readLine()) != null) {
                readLines += readLine + "\n";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return readLines;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_version_info);

        setUpVerinfo();
        setUpLicenseInfo();

    }
}
