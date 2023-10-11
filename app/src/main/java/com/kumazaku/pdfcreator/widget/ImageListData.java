package com.kumazaku.pdfcreator.widget;

import android.os.Parcel;
import android.os.Parcelable;

import com.kumazaku.pdfcreator.utility.Utility;

import java.io.FileNotFoundException;

/**
 * Created by kuma on 2017/01/08.
 */

public class ImageListData implements Parcelable {

    private String path;
    private String name;

    public  ImageListData(String path) throws FileNotFoundException {
        this.path = path;
        this.name = Utility.getFileNameFromPath(path);
    }
    public ImageListData(Parcel in) {
        this.path = in.readString();
        this.name = in.readString();
    }


    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public void setPath(String path) {
        this.path = path;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(path);
        out.writeString(name);
    }

    public static final Parcelable.Creator CREATOR
            = new Parcelable.Creator() {
        public ImageListData createFromParcel(Parcel in) {
            return new ImageListData(in);
        }

        public ImageListData[] newArray(int size) {
            return new ImageListData[size];
        }
    };

}

