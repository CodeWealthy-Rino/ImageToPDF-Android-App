package com.kumazaku.pdfcreator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.SeekBar;
import android.widget.TextView;

import com.darsh.multipleimageselect.activities.AlbumSelectActivity;
import com.darsh.multipleimageselect.helpers.Constants;
import com.darsh.multipleimageselect.models.Image;
import com.google.android.gms.ads.AdRequest;
import com.kumazaku.pdfcreator.exceptions.DuplicatedFileException;
import com.kumazaku.pdfcreator.exceptions.UnsupportedFileFormatException;
import com.kumazaku.pdfcreator.exceptions.UnsupportedFolderException;
import com.kumazaku.pdfcreator.utility.LogHelper;
import com.kumazaku.pdfcreator.utility.Utility;
import com.kumazaku.pdfcreator.widget.ImageListAdapter;
import com.kumazaku.pdfcreator.widget.ImageListData;

import org.json.JSONArray;
import org.json.JSONException;
import org.w3c.dom.Text;

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
import androidx.appcompat.app.AppCompatActivity;

public class PDFCreateActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    static {
        System.loadLibrary("libharu-jni");
    }
    private native int PDFInit();
    private native int PDFAddJpeg(int width, int height , String path);
    private native int PDFSave(String path);

    private boolean isValidName = false;

    /**
     *  読み込んだ画像のリスト
     */
    private ArrayList<ImageListData> images = new ArrayList<>();;

    private void refreshEditNameField()
    {
        TextView textView = (TextView)findViewById(R.id.filename_result_view);
        if (textView != null)
        {
            EditText fileEditView = (EditText)findViewById(R.id.file_prefix_edit_view);
            if (fileEditView != null) {

                String pdfFileName = fileEditView.getText().toString();

                StringBuilder builder = new StringBuilder();
                builder.append(pdfFileName);


                CheckBox dayCheckBox = (CheckBox)findViewById(R.id.day_checkbox);
                if (dayCheckBox.isChecked()) {
                    String timeStamp = new SimpleDateFormat("_yyyyMMdd").format(new Date());
                    builder.append(timeStamp);
                }

                CheckBox timeCheckBox = (CheckBox)findViewById(R.id.time_checkbox);
                if (timeCheckBox.isChecked()) {
                    String timeStamp = new SimpleDateFormat("_HHmmss").format(new Date());
                    builder.append(timeStamp);
                }

                builder.append(".pdf");
                textView.setText(builder.toString());

                Button pdfCreateButton = (Button)findViewById(R.id.pdf_button);

                TextView fileNameCautionView = (TextView)findViewById(R.id.filename_error_view);
                if (fileNameCautionView != null)
                {
                    if (duplicateCheck(new File(saveDir(), builder.toString()).getAbsolutePath()))
                    {
                        String thisFileAlreadyUsed = getString(R.string.this_file_already_used);

                        fileNameCautionView.setText(thisFileAlreadyUsed);
                        pdfCreateButton.setEnabled(false);
                        isValidName = false;
                    }else{
                        fileNameCautionView.setText("");
                        pdfCreateButton.setEnabled(true);
                        isValidName = true;
                    }
                }
            }
        }
    }

    private void registerFileNameEditView()
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

        EditText fileEditView = (EditText)findViewById(R.id.file_prefix_edit_view);
        if (fileEditView != null) {
            fileEditView.setFilters(new InputFilter[]{filter});
            fileEditView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    refreshEditNameField();
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    refreshEditNameField();
                }
            });
        }


        CheckBox dayCheckBox = (CheckBox)findViewById(R.id.day_checkbox);
        if (dayCheckBox != null)
        {
            dayCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    refreshEditNameField();
                }
            });
        }

        CheckBox timeCheckBox = (CheckBox)findViewById(R.id.time_checkbox);
        if (timeCheckBox != null)
        {
            timeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    refreshEditNameField();
                }
            });
        }

        refreshEditNameField();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("images", images);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfcreate);

        // add List View event
        ListView imageList = (ListView) findViewById(R.id.image_list);
        ImageListAdapter adapter =  new ImageListAdapter(this, 0, images);
        imageList.setOnItemClickListener(this);
        imageList.setAdapter(adapter);

        if (savedInstanceState != null) {
            images = savedInstanceState.getParcelableArrayList("images");
        }

        Button cancelButton = (Button) findViewById(R.id.pdf_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                images.clear();
                finish();
            }
        });



        // add pdf creates event
        Button pdfButton = (Button) findViewById(R.id.pdf_button);
        pdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProgressDialog dialog = new ProgressDialog(PDFCreateActivity.this);
                dialog.setOnShowListener( new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(final DialogInterface dialog) {
                        final Handler handler = new Handler();
                        Thread pdfCreateThread = new Thread(new Runnable() {
                            public void run(){
                                try {
                                    PDFInit();
                                    final File dest =  createSaveFile();
                                    for (ImageListData data : images) {
                                        File file = createTempImageFile();
                                        convertToJpeg(data.getPath(), file);
                                        List<Integer> size = jpegSize(file.getAbsolutePath());
                                        PDFAddJpeg(size.get(0), size.get(1), file.getAbsolutePath());
                                        file.delete();
                                    }
                                    PDFSave(dest.getAbsolutePath());

                                    final String pdfCreationTitle = getString(R.string.pdf_complete_message);
                                    final String modifiedPdfCreationTitle = pdfCreationTitle.replace("PDFFILENAME",dest.getName());

                                    final String pdfGuildeLabel = getString(R.string.view_send_guide);

                                    final String completeLabel = getString(R.string.complete);

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialog.dismiss();
                                            if (dest != null) {
                                                new AlertDialog.Builder(PDFCreateActivity.this)
                                                        .setTitle(modifiedPdfCreationTitle)
                                                        .setMessage(pdfGuildeLabel)
                                                        .setNegativeButton(completeLabel, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                images.clear();
                                                                finish();
                                                            }
                                                        })
                                                        .show();

                                                images.clear();
                                                refreshImageList();
                                            }
                                        }
                                    });

                                }catch (IOException e) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialog.dismiss();
                                            Utility.showAlertDialog(getString(R.string.file_write_error), PDFCreateActivity.this);
                                        }
                                    });
                                }
                            }
                        });

                        pdfCreateThread.start();
                    }
                });

                dialog.setMessage(getString(R.string.progress_dialog_message));
                dialog.setTitle(getString(R.string.progress_dialog_title));
                dialog.show();
            }
        });

        Button addMoreImageButton = (Button)findViewById(R.id.add_more_image_button);
        if (addMoreImageButton != null)
        {
            addMoreImageButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(PDFCreateActivity.this, AlbumSelectActivity.class);
                            intent.putExtra(Constants.INTENT_EXTRA_LIMIT, 1000);
                            startActivityForResult(intent, Constants.REQUEST_CODE);
                        }
                    }
            );
        }

        // register compression seekbar
        SeekBar compressionSeekBar = (SeekBar)findViewById(R.id.compressionSeekbar);
        compressionSeekBar.setMax(99);




        registerFileNameEditView();

        Intent intent = new Intent(PDFCreateActivity.this, AlbumSelectActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_LIMIT, 1000);
        startActivityForResult(intent, Constants.REQUEST_CODE);


        refreshImageList();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //
        // Serialize into json
        //
        JSONArray array = new JSONArray();
        int i = 0;
        for (ImageListData data : images) {
            try {
                array.put(i, data.getPath());
                i++;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //
        // Store
        //
        SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences("shared_preference", Context.MODE_PRIVATE).edit();
        editor.putString("imagePaths", array.toString());

        CheckBox dayCheckBox = (CheckBox)findViewById(R.id.day_checkbox);
        CheckBox timeCheckBox = (CheckBox)findViewById(R.id.time_checkbox);
        EditText fileEditView = (EditText)findViewById(R.id.file_prefix_edit_view);
        SeekBar  seekBar = (SeekBar)findViewById(R.id.compressionSeekbar);

        editor.putString("prefixState", fileEditView.getText().toString());

        editor.putBoolean("timeCheckBoxState", timeCheckBox.isChecked());
        editor.putBoolean("dayCheckBoxState", dayCheckBox.isChecked());
        editor.putInt("compression", seekBar.getProgress());


        editor.commit();
    }

    /**
     *  @brief ImageListDataをパスで検索する。
     *  見つからなかった場合は-1を返す。
     */
    private boolean isPathExistsAtImageListData(String path) {
        for (ImageListData data : images) {
            if (data.getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }
    @Override
    protected void onResume() {
        super.onResume();

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
        //
        // restore json
        //
        String stringList = bundle.getString("imagePaths");
        if (stringList != null) {
            try {
                JSONArray array = new JSONArray(stringList);
                for (int i = 0, length = array.length(); i < length; i++) {

                    String path = array.optString(i);
                    if (new File(path).exists() && isPathExistsAtImageListData(path)  == false) {
                        images.add(new ImageListData(path));
                    }
                }
            } catch (JSONException e1) {
                e1.printStackTrace();
            } catch (FileNotFoundException e) {

            }
        }

        String prefixInputState = bundle.getString("prefixState");
        if (prefixInputState == null)
        {
            prefixInputState = "NAME";
        }

        Boolean timeCheckBoxState = bundle.getBoolean("timeCheckBoxState", true);
        Boolean dayCheckBoxState  = bundle.getBoolean("dayCheckBoxState", true);

        CheckBox dayCheckBox = (CheckBox)findViewById(R.id.day_checkbox);
        CheckBox timeCheckBox = (CheckBox)findViewById(R.id.time_checkbox);
        EditText fileEditView = (EditText)findViewById(R.id.file_prefix_edit_view);

        dayCheckBox.setChecked(dayCheckBoxState);
        timeCheckBox.setChecked(timeCheckBoxState);
        fileEditView.setText(prefixInputState);

        int compression = bundle.getInt("compression", 30);
        SeekBar compressionSeekBar = (SeekBar)findViewById(R.id.compressionSeekbar);
        compressionSeekBar.setProgress(compression);

        refreshImageList();
    }

    /**
     * @brief ImageListへの追加を行う
     */
    private void addImageToList(String path) throws FileNotFoundException,
            com.kumazaku.pdfcreator.exceptions.UnsupportedFileFormatException,
            com.kumazaku.pdfcreator.exceptions.DuplicatedFileException,
            com.kumazaku.pdfcreator.exceptions.UnsupportedFolderException {
        {
            if (com.kumazaku.pdfcreator.utility.Utility.getMimeType(path).equals("image/jpeg") ||
                    com.kumazaku.pdfcreator.utility.Utility.getMimeType(path).equals("image/bmp") ||
                    com.kumazaku.pdfcreator.utility.Utility.getMimeType(path).equals("image/gif") ||
                    com.kumazaku.pdfcreator.utility.Utility.getMimeType(path).equals("image/jpg") ||
                    com.kumazaku.pdfcreator.utility.Utility.getMimeType(path).equals("image/png") ||
                    com.kumazaku.pdfcreator.utility.Utility.getMimeType(path).equals("image/webp") ||
                    com.kumazaku.pdfcreator.utility.Utility.getMimeType(path).equals("image/jpeg") ||
                    com.kumazaku.pdfcreator.utility.Utility.getMimeType(path).equals("image/tiff")  ||
                    com.kumazaku.pdfcreator.utility.Utility.getMimeType(path).equals("image/tiff-fx") ) {

                if (new File(path).exists()) {
                    images.add(new ImageListData(path));
                }
            }else{
                throw  new com.kumazaku.pdfcreator.exceptions.UnsupportedFileFormatException();
            }
        }
        {
            refreshImageList();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (data == null) {
            if (this.images.size() == 0)
            {
                finish();
            }
            return;
        }
        try {
            switch (requestCode) {

                case Constants.REQUEST_CODE:
                    ArrayList<Image> images = data.getParcelableArrayListExtra(Constants.INTENT_EXTRA_IMAGES);
                    if (images != null) {
                        for (Image image : images) {
                            addImageToList(image.path);
                        }
                    }

                    if (this.images.size() == 0)
                    {
                        finish();
                    }

                    break;

            }
        }catch (UnsupportedFileFormatException exception) {
            LogHelper.e("Unsupported file format");
            Utility.showAlertDialog(getString(R.string.unsupported_format_message), this);
        }catch (FileNotFoundException exception) {
            LogHelper.e("File Not Found");
            Utility.showAlertDialog(getString(R.string.file_not_found_message), this);
        }catch (DuplicatedFileException exception) {
            LogHelper.e("Duplicated file exception");
            Utility.showAlertDialog(getString(R.string.duplicated_file_message), this);
        }catch (UnsupportedFolderException exception) {
            LogHelper.e("UnsupportedFolderException  exception");
            Utility.showAlertDialog(getString(R.string.unsupported_folder_message), this);
        }
    }
    /**
     * @brief imageListをrefreshする
     */
    private void refreshImageList()
    {
        ListView imageList = (ListView) findViewById(R.id.image_list);
        imageList.invalidateViews();

        Button pdfCreateButton = (Button) findViewById(R.id.pdf_button);
        if (images.size() > 0) {
            if (isValidName) {
                pdfCreateButton.setEnabled(true);
                pdfCreateButton.setClickable(true);
            }
            TextView noImageTextView = (TextView)findViewById(R.id.no_image_text);
            if (noImageTextView != null) {
                noImageTextView.setVisibility(View.INVISIBLE);
            }

        }else{
            pdfCreateButton.setEnabled(false);
            pdfCreateButton.setClickable(false);

            TextView noImageTextView = (TextView)findViewById(R.id.no_image_text);
            if (noImageTextView != null) {
                noImageTextView.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 最終保存用の空ファイルを作る
     *
     * @return 空ファイル
     * @throws IOException
     */
    private File createSaveFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        TextView imageFileNameView = (TextView)findViewById(R.id.filename_result_view);
        String imageFileName = imageFileNameView.getText().toString();
        File storageDir = saveDir();

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File targetFile = new File(storageDir, imageFileName);
        if (targetFile.exists()) {
            targetFile.delete();
        }

        return targetFile;
    }

    /**
     * @brief 保存ファイルを返す
     */
    private File saveDir() {
        String appName = getString(R.string.app_name);
        return Utility.saveFolderPath(appName, this);
    }

    /**
     * 画像を入れるための一時ファイルを作成する
     *
     * @return 画像ファイル
     * @throws IOException
     */
    private File createTempImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        if (storageDir == null) {
            storageDir = getCacheDir();
        }

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        image.deleteOnExit();

        return image;
    }

    /**
     *
     * 画像をJPEG形式に変換してファイルに書き出す
     *
     */
    private void convertToJpeg(String source, File destFile) throws IOException
    {
     //   if (com.kumazaku.pdfcreator.utility.Utility.getMimeType(source).equals("image/jpeg")  ||
     //           com.kumazaku.pdfcreator.utility.Utility.getMimeType(source).equals("image/jpg")) {
    //        Utility.copyFile(new File(source), destFile);
     //   }else{
            OutputStream stream = new FileOutputStream(destFile.getAbsolutePath());
            Bitmap dst =  BitmapFactory.decodeFile(source, null);
            if (dst != null) {
                int compression = ((SeekBar)findViewById(R.id.compressionSeekbar)).getProgress();
                int quality = 100 - compression;


                dst.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                dst.recycle();
            }
            stream.flush();
            stream.close();
      //  }
    }

    /**
     *  returns size of image
     */
    private List<Integer> jpegSize(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        int width  = options.outWidth;
        int height = options.outHeight;

        List<Integer> list = new ArrayList<Integer>();
        list.add(width);
        list.add(height);
        return list;
    }


    /**
     *   ファイル名がだぶっていないかを確認する
     */
    private boolean duplicateCheck(String filePath)
    {
        for (File f : Utility.getListFiles(saveDir(), ".pdf"))
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
        File fullPath = new File(saveDir(), targetFileName);
        if (duplicateCheck(fullPath.getAbsolutePath()) == false) {
            return original.renameTo(fullPath);
        }

        return false;
    }

    /////////////////////////////////////////////////////////////////////////
    // ListView processing
    ////////////////////////////////////////////////////////////////////////

    // タップされたitemの位置
    private int tappedPosition = 0;

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {


        ListView imageList = (ListView) findViewById(R.id.image_list);

        if (parent == imageList) {

            String item = images.get(position).getPath();
            // 選択された位置を保持する
            setPosition(position);
            // アラートダイアログ
            alertCheck(item);
        }
    }

    private void setPosition(int position) {
        tappedPosition = position;
    }

    private int getPosition() {
        return tappedPosition;
    }

    private void alertCheck(String item) {

        String moveUpString = getString(R.string.move_to_up_label);
        String moveDownString = getString(R.string.move_to_down_label);
        String deleteLabel = getString(R.string.delete_label);
        String deleteAllLabel = getString(R.string.delete_all_from_list_label);
        String cancelLabel = getString(R.string.cancel_text);


        String[] alert_menu = {moveUpString, moveDownString, deleteLabel, deleteAllLabel,cancelLabel};

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(item);
        alert.setItems(alert_menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int idx) {
                // リストアイテムを選択したときの処理
                // 上に移動
                if (idx == 0) {
                    moveAbove();
                }
                // 下に移動
                else if (idx == 1) {
                    moveBelow();
                }
                // 削除
                else if (idx == 2) {
                    deleteItem();
                }
                // 全削除
                else if (idx == 3) {
                    images.clear();
                    refreshImageList();
                }
                // cancel
                else {
                    // nothing to do
                }
            }
        });
        alert.show();
    }


    private void moveAbove() {
        int position = getPosition();
        String str = null;
        int temp = 0;
        if (position > 0) {
            Collections.swap(images, position, position - 1);
        } else {
            // top
        }

        refreshImageList();
    }

    private void moveBelow() {
        int position = getPosition();
        String str = null;
        int temp = 0;
        if (position < images.size() - 1) {
            Collections.swap(images, position, position + 1);
        } else {
            // last
        }

        refreshImageList();
    }


    private void deleteItem() {
        int position = getPosition();
        images.remove(position);

        // ListView の更新
        refreshImageList();
    }

}