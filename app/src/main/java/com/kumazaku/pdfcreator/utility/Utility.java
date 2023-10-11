package com.kumazaku.pdfcreator.utility;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import com.kumazaku.pdfcreator.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 汎用ユーティリティ
 */
public class Utility {


    /**
     *  保存フォルダを取得する
     */
    public static File saveFolderPath(String appName, Context context) {
        return context.getFilesDir();
    }

    public static File oldSaveFolderPath(String appName) {
        File   folder  = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString(), appName);
        return folder;
    }


    /**
     * 特定フォルダの中身を列挙
     * @param parentDir
     * @return
     */
    public static List<File> getListFiles(File parentDir, String ext) {
        ArrayList<File> inFiles = new ArrayList<>();
        File[] files = parentDir.listFiles();
         if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    inFiles.addAll(getListFiles(file, ext));
                } else {
                    if (file.getName().endsWith(ext)) {
                        inFiles.add(file);
                    }
                }
            }
        }


        Comparator comparator=new Comparator(){
            public int compare(Object o1,Object o2){
                File f1=(File)o1;
                File f2=(File)o2;

                return (int)(f2.lastModified()-f1.lastModified());
            }
        };
        Collections.sort(inFiles, comparator);
        return inFiles;
    }


    /**
     * メッセージ表示
     */
    public static void showAlertDialog(String message, Context context) {

        AlertDialog.Builder alertDlg = new AlertDialog.Builder(context);
        alertDlg.setTitle(context.getString(R.string.alert_dialog_title));
        alertDlg.setMessage(message);
        alertDlg.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        alertDlg.create().show();
    }

        /**
         * @brief ファイルの存在チェック
         * ファイルが存在しない場合に例外を出す
         * @param path  ファイルパス
         * @exception  @{link FileNotFoundException} ファイルが存在しない場合
         */
    public static void throwIfFileNotExists(String path)  throws FileNotFoundException {

        if (path == null) {
            throw new FileNotFoundException();
        }

        if (new File(path).exists() == false) {
            throw new FileNotFoundException();
        }
    }

    /**
     * @brief ファイルパスからファイル名を取得する
     * @param path  ファイルパス
     * @param path
     * @throws FileNotFoundException ファイルが存在しない場合
     */
    public static String getFileNameFromPath(String path) throws FileNotFoundException {
        throwIfFileNotExists(path);
        File file = new File(path);
        return file.getName();
    }

    /**
     * @brief UriからPathへの変換処理
     * @param uri
     * @return Path文字列
     */
    public static String convertToPath(final Context context, final Uri uri) {

        /*
        String path = null;

        if (uri !=null && "content".equals(uri.getScheme())) {

            String[] columns = {MediaStore.Images.Media.DATA};
            Cursor cursor = contentResolver.query(uri, columns, null, null, null);
            int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            if (cursor == null) {
                assert (false);
            }
            cursor.moveToFirst();
            path = cursor.getString(index);
            cursor.close();

        }else{
            path = uri.getPath();
        }

        return path;
        */
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    /**
     * @brief ファイルパスからMIMEタイプを取得
     * @param path ファイルパス
     */
    public static String getMimeType(String path) {
        File file = new File(path);
        String fn = file.getName();
        int ch = fn.lastIndexOf('.');
        String ext = (ch>=0)?fn.substring(ch + 1):null;

        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
    }

    static final String FILE_PREFIX = "IMAGE";
    static final String FILE_SUFFIX = ".jpg";
    /**
     * @brief ファイルをコピーする
     * @param inFile
     * @param targetDir
     */
    public static File copyFile(File inFile, File outFile) throws  FileNotFoundException, IOException {

        InputStream in   = null;
        OutputStream out = null;

        if (inFile.exists() == false) {
            return null;
        }

        in = new FileInputStream(inFile.getAbsolutePath());
        out = new FileOutputStream(outFile.getAbsolutePath());

        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        in = null;

        // write the output file
        out.flush();
        out.close();
        out = null;

        return outFile;
    }
}
