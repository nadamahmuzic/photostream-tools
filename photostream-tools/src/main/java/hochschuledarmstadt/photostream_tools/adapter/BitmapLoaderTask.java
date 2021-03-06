/*
 * The MIT License
 *
 * Copyright (c) 2016 Andreas Schattney
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hochschuledarmstadt.photostream_tools.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import hochschuledarmstadt.photostream_tools.BitmapUtils;


class BitmapLoaderTask extends AsyncTask<Void, Void, Bitmap> {

    private static final int TYPE_ASSET = -1;
    private static final int TYPE_FILE = -2;
    private static final int TYPE_OTHER = -3;

    private LruBitmapCache lruBitmapCache;
    private WeakReference<ImageView> imageViewReference;
    private final int photoId;
    private OnImageLoadedListener listener;
    private File imageFile;


    private boolean shouldAnimate = false;

    public boolean getShouldAnimate() {
        return shouldAnimate;
    }

    public void setShouldAnimate(boolean shouldAnimate) {
        this.shouldAnimate = shouldAnimate;
    }

    public int getPhotoId() {
        return photoId;
    }

    public BitmapLoaderTask(LruBitmapCache lruBitmapCache, ImageView imageView, int photoId, File imageFile, OnImageLoadedListener listener) {
        this.lruBitmapCache = lruBitmapCache;
        this.imageViewReference = new WeakReference<>(imageView);
        this.photoId = photoId;
        this.listener = listener;
        this.imageFile = new File(imageFile.getAbsolutePath());
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        listener.onTaskStarted(this);
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        Bitmap bitmap = null;
        try {
            Integer key = Integer.valueOf(photoId);
            bitmap = lruBitmapCache.get(key);
            if (bitmap == null) {
                bitmap = decodeBitmapFromFile(imageFile);
                lruBitmapCache.put(key, bitmap);
            } else
                Log.d(BasePhotoAdapter.class.getName(), "Bitmap reused for photo id: " + photoId);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        if (listener != null)
            listener.onTaskFinishedOrCanceled(this, null);
        clear();
    }

    private void clear() {
        if (imageViewReference != null)
            imageViewReference.clear();
        imageFile = null;
        listener = null;
        lruBitmapCache = null;
    }

    @Override
    protected void onCancelled(Bitmap bitmap) {
        super.onCancelled(bitmap);
        if (listener != null)
            listener.onTaskFinishedOrCanceled(this, null);
        clear();
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);

        boolean ok = !isCancelled();

        if (ok && imageViewReference != null) {
            final ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                final BitmapLoaderTask bitmapWorkerTask = getBitmapLoaderTaskRefFrom(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    lruBitmapCache.referenceIncrease(photoId);
                    imageView.setImageBitmap(bitmap);
                    imageView.setTag(photoId);
                    listener.onTaskFinishedOrCanceled(this, imageView);
                }else{
                    listener.onTaskFinishedOrCanceled(this, null);
                }
            }else{
                listener.onTaskFinishedOrCanceled(this, null);
            }
        }else{
            listener.onTaskFinishedOrCanceled(this, null);
            clear();
        }

    }

    static BitmapLoaderTask getBitmapLoaderTaskRefFrom(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapLoaderTask();
            }
        }
        return null;
    }

    private static Bitmap decodeBitmapFromFile(File file) throws FileNotFoundException {
        return internalDecodeBitmap(null, Uri.fromFile(file), TYPE_FILE);
    }

    private static InputStream createInputStream(Context context, Uri uri, int type) throws IOException {
        switch(type){
            case TYPE_ASSET:
                return context.getAssets().open(uri.toString().replace("assets://", ""));
            case TYPE_FILE:
                try{
                    return new FileInputStream(uri.toString());
                }catch(Exception e){
                    return new FileInputStream(new File(Uri.decode(uri.toString()).replace("file://","")));
                }
            case TYPE_OTHER:
                return context.getContentResolver().openInputStream(uri);
        }
        return null;
    }

    private static Bitmap internalDecodeBitmap(Context context, Uri uri, int type) throws FileNotFoundException {
        Bitmap bm = null;
        try {
            BitmapFactory.Options options = lessResolution(createInputStream(context, uri, type), 450, 450);
            bm = BitmapFactory.decodeStream(createInputStream(context, uri, type), null, options);
            if (context != null) {
                ExifInterface exif = new ExifInterface(getRealPathFromURI(context, uri));
                String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
                int orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;

                int rotationAngle = 0;
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;

                if (rotationAngle != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationAngle);
                    Bitmap bmCopy = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
                    bm.recycle();
                    bm = bmCopy;
                }
            }
        } catch (IOException e) {
            Log.e(BitmapLoaderTask.class.getName(), e.toString(), e);
        }
        return bm;
    }

    private static BitmapFactory.Options lessResolution (InputStream is, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        // First decode with inJustDecodeBounds=true to check dimensions
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return options;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round(((float) height / (float) reqHeight) + 0.25f);
            final int widthRatio = Math.round(((float) width / (float) reqWidth) + 0.25f);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    private static String getRealPathFromURI(Context context, Uri contentURI) {
        String result;
        Cursor cursor = context.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    public WeakReference<ImageView> getImageViewReference() {
        return imageViewReference;
    }
}
