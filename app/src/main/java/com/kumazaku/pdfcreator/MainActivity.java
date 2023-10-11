package com.kumazaku.pdfcreator;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.darsh.multipleimageselect.activities.AlbumSelectActivity;
import com.darsh.multipleimageselect.helpers.Constants;
import com.darsh.multipleimageselect.models.Image;
import com.google.android.gms.ads.AdRequest;
import com.kumazaku.pdfcreator.exceptions.DuplicatedFileException;
import com.kumazaku.pdfcreator.exceptions.UnsupportedFileFormatException;
import com.kumazaku.pdfcreator.exceptions.UnsupportedFolderException;
import com.kumazaku.pdfcreator.utility.LogHelper;
import com.kumazaku.pdfcreator.utility.Utility;
import com.kumazaku.pdfcreator.versionInfo.VersionInfoActivity;
import com.kumazaku.pdfcreator.widget.ImageListAdapter;
import com.kumazaku.pdfcreator.widget.ImageListData;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {



    private int     reorderCheckedItem = 0;
    private boolean dayCheckBoxState = false;
    private boolean timeCheckBoxState = false;
    private String  prefixInputState  = null;

    private int numLaunched = 0;

    /**
     *  作成したPDFのリスト
     */
    private ArrayList<String> pdfs = new ArrayList<>();


    void checkPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Android 6.0 のみ、該当パーミッションが許可されていない場合
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // パーミッションが必要であることを明示するアプリケーション独自のUIを表示
            }

            final int REQUEST_CODE = 1;
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, REQUEST_CODE);
        }

        int permissionCheck2 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
            // Android 6.0 のみ、該当パーミッションが許可されていない場合
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // パーミッションが必要であることを明示するアプリケーション独自のUIを表示
            }

            final int REQUEST_CODE = 1;
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_CODE);
        }

    }


    /**
     * @brief imageListをrefreshする
     */
    private void refreshImageList()
    {
        if (pdfs.size() > 0) {
            TextView noPDFTextView = (TextView)findViewById(R.id.no_pdf_text);
            if (noPDFTextView != null) {
                noPDFTextView.setVisibility(View.INVISIBLE);
            }

            TextView pdfDesc = (TextView) findViewById(R.id.description_view);
            if (pdfDesc != null)
            {
                pdfDesc.setVisibility(View.VISIBLE);
            }

        }else{
            TextView noPDFTextView = (TextView)findViewById(R.id.no_pdf_text);
            if (noPDFTextView != null) {
                noPDFTextView.setVisibility(View.VISIBLE);
            }

            TextView pdfDesc = (TextView) findViewById(R.id.description_view);
            if (pdfDesc != null)
            {
                pdfDesc.setVisibility(View.INVISIBLE);
            }
        }
    }


    /**
     * @brief pdfListをreloadする
     */
    private void reloadPDFList()
    {
        pdfs.clear();

        for (File f : Utility.getListFiles(saveDir(this), ".pdf")) {
            pdfs.add(f.getName());
        }

        ListView imageList = (ListView) findViewById(R.id.pdf_list);
        imageList.invalidateViews();
    }

    /**
     *   ファイル名がだぶっていないかを確認する
     */
    private boolean duplicateCheck(String filePath)
    {
         for (File f : Utility.getListFiles(saveDir(this), ".pdf"))
         {
             if(f.getAbsolutePath().compareTo(filePath) == 0)
             {
                 return true;
             }
         }
        return false;
    }

    /**
     *  ファイル名のリネームをする
     */
    private boolean renameFile(File original, String targetFileName)
    {
        File fullPath = new File(saveDir(this), targetFileName);
        if (duplicateCheck(fullPath.getAbsolutePath()) == false) {
            return original.renameTo(fullPath);
        }

        return false;
    }


    /**
     * @brief 保存ファイルを返す
     */
    private File saveDir(Activity context) {
        String appName = getString(R.string.app_name);
        return Utility.saveFolderPath(appName, context);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret = super.onCreateOptionsMenu(menu);
        menu.add(0 , Menu.FIRST , Menu.NONE , "About this app");
        return ret;
    }

    // メニューアイテム選択イベント
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Menu.FIRST:

                Intent intent = new Intent(MainActivity.this, VersionInfoActivity.class);
                startActivityForResult(intent, 0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences("shared_preference", Context.MODE_PRIVATE).edit();

        if (prefixInputState != null) {
            editor.putString("prefixState", prefixInputState);
        }
        editor.putBoolean("timeCheckBoxState", timeCheckBoxState);
        editor.putBoolean("dayCheckBoxState", dayCheckBoxState);
        editor.putInt("reorderCheckedItem", reorderCheckedItem);

        numLaunched++;
        editor.putInt("numLaunched", numLaunched);

        editor.commit();
    }


    @Override
    protected void onResume() {
        super.onResume();


        reloadPDFList();

        //
        // jsonのデータを復元する
        //
        Bundle bundle = new Bundle();
        Map<String, ?> prefKV = getApplicationContext().getSharedPreferences("shared_preference", Context.MODE_PRIVATE).getAll();
        Set<String> keys = prefKV.keySet();
        for(String key : keys){
            Object value = prefKV.get(key);
            if(value instanceof String){
                bundle.putString(key, (String) value);
            }
            if (value instanceof  Boolean){
                bundle.putBoolean(key, (boolean) value);
            }

            if (value instanceof Integer){
                bundle.putInt(key, (int) value);
            }
        }


        prefixInputState = bundle.getString("prefixState");
        if (prefixInputState == null)
        {
            prefixInputState = "PDF";
        }

        timeCheckBoxState = bundle.getBoolean("timeCheckBoxState");
        dayCheckBoxState  = bundle.getBoolean("dayCheckBoxState");
        reorderCheckedItem = bundle.getInt("reorderCheckedItem");
        numLaunched = bundle.getInt("numLaunched");


        refreshImageList();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();


        // add PDF View
        ListView pdfList = (ListView) findViewById(R.id.pdf_list);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, R.layout.pdf_layout, pdfs);
        pdfList.setAdapter(adapter2);
        pdfList.setOnItemClickListener(this);

        // add Album select event
        Button button = (Button) findViewById(R.id.load_image_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PDFCreateActivity.class);
                startActivityForResult(intent, Constants.REQUEST_CODE);
            }
        });

        final Button reorderButton = (Button)findViewById(R.id.reorder_button);
        if (reorderButton != null)
        {
            reorderButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    String dateCreateString = getString(R.string.reorder_by_date);
                    String nameString = getString(R.string.reorder_by_name);
                    String sizeString = getString(R.string.reorder_by_size);

                    String reorderString = getString(R.string.reorder_pdf_label);

                    String completeString = getString(R.string.complete);
                    String cancelString = getString(R.string.cancel_text);

                     final String[] items = {dateCreateString, nameString, sizeString};
                        int defaultItem = reorderCheckedItem; // デフォルトでチェックされているアイテム
                        final List<Integer> checkedItems = new ArrayList<>();
                        checkedItems.add(defaultItem);
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(reorderString)
                                .setSingleChoiceItems(items, defaultItem, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        checkedItems.clear();
                                        checkedItems.add(which);
                                    }
                                })
                                .setPositiveButton(completeString, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (!checkedItems.isEmpty()) {
                                        }

                                        reorderCheckedItem = checkedItems.get(0);
                                    }
                                })
                                .setNegativeButton(cancelString, null)
                                .show();

                }
            });
        }

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });



        refreshImageList();

        String appName = getString(R.string.app_name);
        if (Utility.oldSaveFolderPath(appName).exists()) {
            showFilePlaceCaution(Utility.oldSaveFolderPath(appName).getAbsolutePath());
        }
    }

    void showFilePlaceCaution(String oldFolderPath)
    {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        LayoutInflater adbInflater = LayoutInflater.from(this);
        View eulaLayout = adbInflater.inflate(R.layout.alert_dialog_with_checkbox, null);
        SharedPreferences settings = getSharedPreferences("shared_preference", 0);
        String skipMessage = settings.getString("skip_folder_caution_message", "NOT checked");

        String messageTitle = getString(R.string.notice);
        String messageBody = getString(R.string.folder_moved_message);

        messageBody = messageBody.replace("PATH", oldFolderPath);

        CheckBox dontShowAgain = (CheckBox) eulaLayout.findViewById(R.id.skip);
        adb.setView(eulaLayout);
        adb.setTitle(messageTitle);
        adb.setMessage(messageBody);

        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String checkBoxResult = "NOT checked";

                if (dontShowAgain.isChecked()) {
                    checkBoxResult = "checked";
                }

                SharedPreferences settings = getSharedPreferences("shared_preference", 0);
                SharedPreferences.Editor editor = settings.edit();

                editor.putString("skip_folder_caution_message", checkBoxResult);
                editor.commit();

                // Do what you want to do on "OK" action

                return;
            }
        });


        if (!skipMessage.equals("checked")) {
            adb.show();
        }
    }


    /////////////////////////////////////////////////////////////////////////
    // ListView processing
    ////////////////////////////////////////////////////////////////////////

    // タップされたitemの位置
    private int tappedPosition = 0;

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {


        ListView pdfList   = (ListView) findViewById(R.id.pdf_list);

        if (parent == pdfList) {
            String item = pdfs.get(position);
            pdfCheck(item);
        }
    }


    private void pdfCheck(final String item) {

        String viewLabel = getString(R.string.view_label);
        String transferLabel = getString(R.string.transfer_label);
        final String deleteLablel = getString(R.string.delete_label);
        final String deleteAllLabel = getString(R.string.delete_all_label);
        String changeNameLabel = getString(R.string.change_file_name);
        String cancelLabel = getString(R.string.cancel_text);
        final String deleteConfirmMessage = getString(R.string.delete_confirm_message);
        final String deleteAllConfirmMes = getString(R.string.delete_all_confirm_message);

        String[] alert_menu = {viewLabel,
                               transferLabel,
                               deleteLablel,
                               deleteAllLabel,
                                changeNameLabel,
                                cancelLabel};

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(item);
        alert.setItems(alert_menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int idx) {

                final File file = new File(saveDir(MainActivity.this), item);
                if (file.exists() == false) {
                    return;
                }

                

                Uri uri = FileProvider.getUriForFile(
                        MainActivity.this
                        ,getApplicationContext().getPackageName() + ".provider"
                        , file);

                // 表示
                if (idx == 0) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                }

                // 転送
                if (idx == 1 ) {
                    Intent intent = new Intent(Intent.ACTION_SEND)
                            .setType("application/pdf")
                            .putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(intent);
                }

                // 削除
                if (idx == 2) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                    // AlertDialogのタイトル設定します
                    alertDialogBuilder.setTitle(deleteLablel);
                    // AlertDialogのメッセージ設定
                    alertDialogBuilder.setMessage(deleteConfirmMessage);
                    // AlertDialogのYesボタンのコールバックリスナーを登録
                    alertDialogBuilder.setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    file.delete();
                                    reloadPDFList();
                                }
                            });
                    // AlertDialogのNoボタンのコールバックリスナーを登録
                    alertDialogBuilder.setNeutralButton("No",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    // AlertDialogのキャンセルができるように設定
                    alertDialogBuilder.setCancelable(true);

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    // AlertDialogの表示
                    alertDialog.show();
                }

                // 全削除
                if (idx == 3) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                    // AlertDialogのタイトル設定します
                    alertDialogBuilder.setTitle(deleteAllLabel);
                    // AlertDialogのメッセージ設定
                    alertDialogBuilder.setMessage(deleteAllConfirmMes);
                    // AlertDialogのYesボタンのコールバックリスナーを登録
                    alertDialogBuilder.setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    for (String name : pdfs) {
                                        File toDeleteFile = new File(saveDir(MainActivity.this), name);
                                        if (toDeleteFile.exists()) {
                                            toDeleteFile.delete();
                                        }
                                    }

                                    reloadPDFList();
                                }
                            });
                    // AlertDialogのNoボタンのコールバックリスナーを登録
                    alertDialogBuilder.setNeutralButton("No",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    // AlertDialogのキャンセルができるように設定
                    alertDialogBuilder.setCancelable(true);

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    // AlertDialogの表示
                    alertDialog.show();
                }


                // ファイル名を変更
                if (idx == 4) {
                    showFileNameEditDialog(file);
                }


            }
        });
        alert.show();
    }

    private void setPosition(int position) {
        tappedPosition = position;
    }

    private int getPosition() {
        return tappedPosition;
    }
    private void refreshEditDialog(Dialog dialog)
    {
        TextView textView = (TextView)dialog.findViewById(R.id.filename_result_view);
        if (textView != null)
        {
            EditText fileEditView = (EditText)dialog.findViewById(R.id.file_prefix_edit_view);
            if (fileEditView != null) {

                String pdfFileName = fileEditView.getText().toString();

                StringBuilder builder = new StringBuilder();
                builder.append(pdfFileName);

                prefixInputState = pdfFileName;

                CheckBox dayCheckBox = (CheckBox)dialog.findViewById(R.id.day_checkbox);
                if (dayCheckBox.isChecked()) {
                    String timeStamp = new SimpleDateFormat("_yyyyMMdd").format(new Date());
                    builder.append(timeStamp);

                    dayCheckBoxState = dayCheckBox.isChecked();
                }else{
                    dayCheckBoxState = false;
                }

                CheckBox timeCheckBox = (CheckBox)dialog.findViewById(R.id.time_checkbox);
                if (timeCheckBox.isChecked()) {
                    String timeStamp = new SimpleDateFormat("_HHmmss").format(new Date());
                    builder.append(timeStamp);

                    timeCheckBoxState = timeCheckBox.isChecked();
                }else{
                    timeCheckBoxState = false;
                }

                builder.append(".pdf");
                textView.setText(builder.toString());

                String thisFileAlreadyUsed = getString(R.string.this_file_already_used);

                TextView fileNameCautionView = (TextView)dialog.findViewById(R.id.filename_error_view);
                if (fileNameCautionView != null)
                {
                    if (duplicateCheck(new File(saveDir(this), builder.toString()).getAbsolutePath()))
                    {
                        fileNameCautionView.setText(thisFileAlreadyUsed);
                        Button b = (Button)dialog.findViewById(R.id.file_edit_finish_button);
                        b.setEnabled(false);
                    }else{
                        fileNameCautionView.setText("");
                        Button b = (Button)dialog.findViewById(R.id.file_edit_finish_button);
                        b.setEnabled(true);
                    }
                }
            }
        }
    }


    private void showFileNameEditDialog(final File dest)
    {

        InputFilter filter = new InputFilter() {
            String blockCharacterSet = "~*/.";
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (source != null && blockCharacterSet.contains(("" + source))) {
                    return "";
                }
                return null;
            }
        };

        final Dialog dialog = new Dialog(this);

        String inputFileName = getString(R.string.input_filename_label);

        dialog.setTitle(inputFileName);
        dialog.setContentView(R.layout.filename_dialog);

        EditText fileEditView = (EditText)dialog.findViewById(R.id.file_prefix_edit_view);
        if (fileEditView != null) {
            if (prefixInputState != null) {
                fileEditView.setText(prefixInputState);
            }
            fileEditView.setFilters(new InputFilter[]{filter});
            fileEditView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    refreshEditDialog(dialog);
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    refreshEditDialog(dialog);
                }
            });
        }

        Button completeButton = (Button)dialog.findViewById(R.id.file_edit_finish_button);
        if (completeButton != null) {
            completeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TextView resultText = (TextView)dialog.findViewById(R.id.filename_result_view);
                    if (resultText != null)
                    {
                        dialog.dismiss();

                        String fileName = resultText.getText().toString();
                        renameFile(dest, fileName);

                        reloadPDFList();
                    }
                }
            });
        }

        Button cancelButton = (Button)dialog.findViewById(R.id.file_edit_cancel_button);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });
        }


        CheckBox dayCheckBox = (CheckBox)dialog.findViewById(R.id.day_checkbox);
        if (dayCheckBox != null)
        {
            dayCheckBox.setChecked(dayCheckBoxState);
            dayCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    refreshEditDialog(dialog);
                }
            });
        }

        CheckBox timeCheckBox = (CheckBox)dialog.findViewById(R.id.time_checkbox);
        if (timeCheckBox != null)
        {
            timeCheckBox.setChecked(timeCheckBoxState);
            timeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    refreshEditDialog(dialog);
                }
            });
        }

        refreshEditDialog(dialog);
        dialog.show();
    }



}
