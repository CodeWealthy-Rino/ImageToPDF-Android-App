package com.kumazaku.pdfcreator.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kumazaku.pdfcreator.R;

import java.util.List;


/**
 * @brief 画像ファイルをリスト表示するための{@link ListView}用の{@link ArrayAdapter}
 */
public class ImageListAdapter extends ArrayAdapter<ImageListData> {

    private LayoutInflater      layoutInflater_;
    private List<ImageListData> listDatas_;

    private static class ViewHolder {
        private ImageView imageView;
        private TextView  textView;

        public ViewHolder(View view) {
            this.imageView = (ImageView)view.findViewById(R.id.item_image);
            this.textView  = (TextView)view.findViewById(R.id.item_text);
        }
    }

    static final double MAX_SIZE = 200;

    private double shrinkScale(double width, double height) {
        double xScale = MAX_SIZE / width;
        double yScale = MAX_SIZE / height;

        if (xScale > yScale) {

            if (yScale >= 1.0) {
                return  1.0;
            }

            return yScale;
        }

        if (xScale >= 1.0) {
            return  1.0;
        }

        return xScale;
    }


    private Bitmap createBitmap(ImageListData data) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(data.getPath(), options);

        int width  = options.outWidth;
        int height = options.outHeight;

        double scale = shrinkScale( width , height);

        options.inSampleSize       = (int)(1.0 / scale);
        options.inJustDecodeBounds = false;

        Bitmap dst =  BitmapFactory.decodeFile(data.getPath(), options);
        if (dst != null && (dst.getWidth() != (int)(width * scale)  || dst.getHeight() !=  (int)(height * scale)) ) {

            Bitmap newBitmap = Bitmap.createScaledBitmap(dst, (int)(width * scale) , (int)(height * scale), false);
            if(newBitmap != null) {
                dst.recycle();
                dst = newBitmap;
            }
        }

        return dst;
    }

    public ImageListAdapter(Context context, int textViewResourceId, List<ImageListData> listDatas) {
        super(context, textViewResourceId, listDatas);
        layoutInflater_ = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        listDatas_      = listDatas;
    }


    /**
     *   positionの位置に表示したいViewを返す。
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        {
            /*
               convertViewは使い回しされている可能性があるのでnullの時だけ新しく作る
            */
            if (convertView == null) {
                convertView = layoutInflater_.inflate(R.layout.image_layout, null);
                holder      = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
        }
        {
            ImageListData item = getItem(position);
            holder.imageView.setImageBitmap(createBitmap(item));
            holder.textView.setText(item.getName());

        }

        return convertView;
    }


}
