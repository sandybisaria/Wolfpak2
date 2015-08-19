package com.wolfpakapp.wolfpak2.camera.editor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.camera2.CameraCharacteristics;
import android.location.Location;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.wolfpakapp.wolfpak2.WolfpakSQLiteHelper;
import com.wolfpakapp.wolfpak2.WolfpakServiceProvider;
import com.wolfpakapp.wolfpak2.camera.preview.CameraLayout;
import com.wolfpakapp.wolfpak2.service.LocationProvider;
import com.wolfpakapp.wolfpak2.service.NoLocationException;
import com.wolfpakapp.wolfpak2.service.SQLiteManager;
import com.wolfpakapp.wolfpak2.service.UserIdManager;

import org.apache.http.Header;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Handles the saving of media to the phone and to the server
 * @author Roland Fong
 */
public class MediaSaver {

    private static final String TAG = "TAG-MediaSaver";
    /**
     * The server URL
     */
    public static final String SERVER_URL = "http://ec2-52-4-176-1.compute-1.amazonaws.com/posts/";

    /*
     * Keys for database and post for convenience, directly from MediaEntry
     */
    public static final String HANDLE = WolfpakSQLiteHelper.MediaEntry.COLUMN_HANDLE;
    public static final String LATITUDE = WolfpakSQLiteHelper.MediaEntry.COLUMN_LATITUDE;
    public static final String LONGITUDE = WolfpakSQLiteHelper.MediaEntry.COLUMN_LONGITUDE;
    public static final String IS_NSFW = WolfpakSQLiteHelper.MediaEntry.COLUMN_IS_NSFW;
    public static final String IS_IMAGE = WolfpakSQLiteHelper.MediaEntry.COLUMN_IS_IMAGE;
    public static final String USER = WolfpakSQLiteHelper.MediaEntry.COLUMN_USER;
    public static final String THUMBNAIL = WolfpakSQLiteHelper.MediaEntry.COLUMN_THUMBNAIL;
    public static final String MEDIA = WolfpakSQLiteHelper.MediaEntry.COLUMN_MEDIA;

    /**
     * Array of key constants
     */
    public static final String[] keys = {
            HANDLE, LATITUDE, LONGITUDE, IS_NSFW, IS_IMAGE, USER, THUMBNAIL, MEDIA
    };

    /*
     * Video saving service intent parameters
     */
    public static final String VIDEO_PATH = "Video Path";
    public static final String OVERLAY_PATH = "Overlay Path";
    public static final String OUTPUT_PATH = "Output Path";
    public static final String IS_UPLOADING = "Is Uploading";

    private static FFmpeg mFfmpeg;
    private static Activity mActivity;

    private static ProgressDialog mProgressDialog;

    public interface MediaSaverListener {
        public void onDownloadCompleted();
        public void onUploadCompleted();
    };

    /**
     * Notifies when upload or download completes.  Since this is used once during each upload
     * or download, only a single static variable is required.
     */
    private static MediaSaverListener mMediaSaverListener;

    /**
     * A java object representation of either video or image that is to be saved and/or uploaded
     */
    private static class MediaObject    {
        public Bitmap overlay;
        public Bitmap image;
        public String videoPath;

        /**
         * Image constructor
         * @param image
         * @param overlay
         */
        public MediaObject(Bitmap image, Bitmap overlay)    {
            this.overlay = overlay;
            this.image = image;
        }

        /**
         * Video Constructor
         * @param videoPath
         * @param overlay
         */
        public MediaObject(String videoPath, Bitmap overlay)    {
            this.videoPath = videoPath;
            this.overlay = overlay;
        }
    }

    public MediaSaver() {
    }

    /**
     * Downloads the image to user device
     * @param listener the MediaSaverListener
     * @param image the background image
     * @param overlay the foreground overlay
     */
    public static void downloadImage(final MediaSaverListener listener,
                                     final Bitmap image, final Bitmap overlay) {
        mProgressDialog = new ProgressDialog(mActivity);

        final MediaObject imageObject = new MediaObject(image, overlay);
        mMediaSaverListener = listener;

        // use AsyncTask to start a progress dialog
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDialog.setTitle("Saving...");
                mProgressDialog.setMessage("Please Wait.");
                mProgressDialog.setCancelable(false);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                // save image
                try {
                    saveImagetoFile(imageObject);
                } catch(IOException e)  {
                    Toast.makeText(mActivity, "Save encountered an error", Toast.LENGTH_SHORT);
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                // close dialog and notify listeners of completion
                mProgressDialog.dismiss();
                listener.onDownloadCompleted();
            }
        };
        task.execute((Void[])null);
    }

    /**
     * Saves image with overlay to file as image media
     * @param imageObject the object representation of the image
     * @return path the filepath
     * @throws IOException
     */
    private static String saveImagetoFile(MediaObject imageObject) throws IOException   {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "/Wolfpak");
        if(!storageDir.exists()) {
            storageDir.mkdir();
        }
        File imagefile = new File(storageDir, timeStamp + ".jpeg");

        String filePath = imagefile.getAbsolutePath();

        // write data to file system
        FileOutputStream output = new FileOutputStream(imagefile);

        // combines overlay and textureview
        Canvas c = new Canvas(imageObject.image);
        c.drawBitmap(imageObject.overlay, 0, 0, null);
        imageObject.image.compress(Bitmap.CompressFormat.JPEG, 75, output);

        // add metadata
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);
        // insert image
        mActivity.getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // close streams
        if (null != output) {
            try {
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filePath;
    }

    /**
     * Uploads image to server
     * @param listener the MediaSaverListener
     * @param image the background image
     * @param overlay the foreground overlay
     */
    public static void uploadImage(MediaSaverListener listener, Bitmap image, Bitmap overlay)    {

        MediaObject imageObject = new MediaObject(image, overlay);
        mMediaSaverListener = listener;
        ContentValues values = new ContentValues();
        showUploadDialog(values, imageObject, true);
    }

    public static void downloadVideo(MediaSaverListener listener, String videoPath,
                                     Bitmap overlay)  {
        mProgressDialog = new ProgressDialog(mActivity);
        mMediaSaverListener = listener;

        final MediaObject videoObject = new MediaObject(videoPath, overlay);
        Log.d(TAG, "Creating async vid saving task");
        // use AsyncTask to start a progress dialog
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDialog.setTitle("Saving...");
                mProgressDialog.setMessage("Please Wait.");
                mProgressDialog.setCancelable(false);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Log.d(TAG, "about to try saving vid");
                    saveVideotoFile(videoObject, null, false);
                } catch(IOException e)  {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                // close dialog and notify listeners of completion
                Log.d(TAG, "video save onpostexec");
                mProgressDialog.dismiss();
                mMediaSaverListener.onDownloadCompleted();
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    /**
     * Saves video to file system in movies directory
     * @param videoObject the object representation of video
     * @param isUploading whether an upload to server is expected
     * @throws IOException
     */
    private static void saveVideotoFile(MediaObject videoObject, ContentValues values,
                                        boolean isUploading) throws IOException {
        Log.d(TAG, "In save vid to file");
        // make a copy of the video; ensures it will not be overwritten
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File videoFile = new File(mActivity.getExternalFilesDir(null), timeStamp + ".mp4");
        copy(new File(videoObject.videoPath), videoFile);
        Log.d(TAG, "Creating Overlay");

        // create and save overlay
        File tempImgFile = new File(mActivity.getExternalFilesDir(null), timeStamp + ".png");
        FileOutputStream output = new FileOutputStream(tempImgFile);

        // blit overlay onto texture
        Bitmap finalImage = Bitmap.createBitmap(videoObject.overlay);
        Matrix matrix = new Matrix(); // used to rotate the overlay 90 degrees because god knows why...
        matrix.postRotate(-90);
        Bitmap resizedBitmap = Bitmap.createBitmap(
                finalImage, 0, 0, finalImage.getWidth(), finalImage.getHeight(), matrix, true);

        // compresses whatever textureview and overlay have into output
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 60, output);
        Log.d(TAG, "Creating output file");

        // create output file
        String outputPath = "MP4_" + timeStamp + ".mp4";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "/Wolfpak");
        if(!storageDir.exists()) {
            storageDir.mkdir();
        }
        File video = new File(storageDir, outputPath); //File.createTempFile(outputPath, ".mp4", storageDir);
        Log.d(TAG, "About to start service intent");

        // start intent to service to handle this in background
        Intent intent = new Intent(mActivity, VideoSavingService.class);
        intent.putExtra(VIDEO_PATH, videoFile.getCanonicalPath());
        intent.putExtra(OVERLAY_PATH, tempImgFile.getCanonicalPath());
        intent.putExtra(OUTPUT_PATH, video.getCanonicalPath());
        intent.putExtra(IS_UPLOADING, isUploading);
        if(values != null)  {
            for(String key : values.keySet())   {
                if(values.get(key).getClass().equals(String.class))
                    intent.putExtra(key, values.get(key).toString());
                else if(values.get(key).getClass().equals(Double.class))
                    intent.putExtra(key, (Double) values.get(key));
            }
        }
        if(isUploading) {
            // stop the progress dialog here
            mProgressDialog.dismiss();
        }
        mActivity.startService(intent);
    }

    /**
     * Upload video to server
     * @param videoPath the path of the original video
     * @param overlay the foreground overlay
     */
    public static void uploadVideo(MediaSaverListener listener, String videoPath, Bitmap overlay)   {
        //generateUploadParams(listener, null, false, videoPath, overlay);
        MediaObject videoObject = new MediaObject(videoPath, overlay);
        mMediaSaverListener = listener;
        ContentValues values = new ContentValues();
        showUploadDialog(values, videoObject, false);
    }

    /**
     * Reusable function that sends created file (image or video) to server.
     * Generates parameters and starts AsyncHttpClient.
     * @param params the request parameters
     * @param isImage whether the upload is for an image or not
     */
    public static void upload(final RequestParams params, final boolean isImage)   {
        // ensure working network connection
        ConnectivityManager connMgr = (ConnectivityManager)
                mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) { // continue if working connection

            // begin the upload
            AsyncHttpClient client = new AsyncHttpClient();
            client.post(SERVER_URL, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.d(TAG, "Upload Success " + statusCode);
                    // only call if this is image, video already called
                    if(isImage) {
                        mMediaSaverListener.onUploadCompleted();
                        mProgressDialog.dismiss();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.e(TAG, "Upload Failure " + statusCode);
                    // only call if this is image, video already called
                    if(isImage) {
                        mMediaSaverListener.onUploadCompleted();
                        mProgressDialog.dismiss();
                    }
                }
            });
        } else { // alert user if connection fails
            Log.e(TAG, "Couldn't connect to network");
            Toast.makeText(mActivity, "Upload failed: Couldn't connect to network", Toast.LENGTH_SHORT);
        }
    }

    /**
     * Converts a {@link ContentValues} object to {@link RequestParams}.
     * @param values the content values
     * @return requestparams
     */
    public static RequestParams contentValuesToRequestParams(ContentValues values)    {
        RequestParams requestParams = new RequestParams();
        for(String key : values.keySet()) {
            if(key != MEDIA && key != THUMBNAIL)
                requestParams.put(key, values.get(key));
        }
        // media and thumbnail must be handled separately since they are stored as strings in
        // contentvalues but must be converted to files in requestparams
        try {
            requestParams.put(MEDIA, new File(values.get(MEDIA).toString()));
            if(values.get(THUMBNAIL) != null)
                requestParams.put(THUMBNAIL, new File(values.get(THUMBNAIL).toString()));
        } catch(IOException e)  {
            e.printStackTrace();
        }
        return requestParams;
    }

    /**
     * Caches media into SQLite database
     */
    public static void cacheMedia(ContentValues values)    {
        SQLiteManager sqLiteManager = (SQLiteManager) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.SQLITEMANAGER);
        try {
            sqLiteManager.open();
        } catch(SQLException e) {
            e.printStackTrace();
        }

        sqLiteManager.addEntry(values);

        /*********************************************************************/
        // TODO lists sql contents for debugging purposes
        Log.d(TAG, "LISTING SQL CONTENTS:");
        for(String handle : sqLiteManager.getMediaHandles())    {
            Log.d(TAG, "HANDLE: " + handle);
        }
        /*********************************************************************/

        sqLiteManager.close();
    }

    /**
     * Waits 5 seconds.  For stalling progress dialog to buy some time
     */
    private static void stall(final boolean isUpload) {
        (new Thread(new Runnable()  {
            @Override
            public void run() {
                // just wait to buy some time
                try {
                    Thread.sleep(3000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
                mProgressDialog.dismiss();
                if(isUpload) {
                    mMediaSaverListener.onUploadCompleted();
                }
            }
        })).start();
    }

    /**
     * Inputs user and location into {@link ContentValues} object
     * @param values
     * @return updated ContentValues
     */
    private static void generateUploadParams(final ContentValues values)    {
        UserIdManager userIdManager = (UserIdManager) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.USERIDMANAGER);
        String userId = userIdManager.getDeviceId();

        try {
            Location location = ((LocationProvider) WolfpakServiceProvider
                    .getServiceManager(WolfpakServiceProvider.LOCATIONPROVIDER)).getLastLocation();

            values.put(LATITUDE, location.getLatitude());
            values.put(LONGITUDE, location.getLongitude());
        } catch (NoLocationException e) {
            //TODO Handle lack of location
        }

        values.put(USER, userId);
    }

    /**
     * Displays upload dialog prompting user for handle and is_nsfw
     * @param values
     * @param mediaObject
     * @param isImage
     */
    private static void showUploadDialog(final ContentValues values,
                                         final MediaObject mediaObject, final boolean isImage)  {
        Log.d(TAG, "Showing upload dialog");
        UploadDialog uploadDialog = new UploadDialog();
        uploadDialog.setUploadDialogListener(new UploadDialog.UploadDialogListener() {
            @Override
            public void onDialogPositiveClick(UploadDialog dialog) {
                // start progress dialog
                mProgressDialog = ProgressDialog.show(mActivity, "Please Wait...", "Sending...", true);

                // add values
                values.put(HANDLE, dialog.getHandle());
                values.put(IS_NSFW, dialog.isNsfw() ? "true" : "false");
                values.put(IS_IMAGE, isImage ? "true" : "false");

                if(isImage) {
                    // save image
                    try {
                        generateUploadParams(values);
                        String path = saveImagetoFile(mediaObject);
                        values.put(MEDIA, path);
                    } catch (IOException e)   {
                        e.printStackTrace();
                    }
                    // cache and upload
                    cacheMedia(values);
                    upload(contentValuesToRequestParams(values), isImage);
                } else {
                    // save video; service will worry about caching and uploading
                    try {
                        generateUploadParams(values);
                        mMediaSaverListener.onUploadCompleted();
                        saveVideotoFile(mediaObject, values, true);
                    } catch(IOException e)  {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onDialogNegativeClick(UploadDialog dialog) {
            }
        });
        Log.d(TAG, "showing upload dialog");
        uploadDialog.show(mActivity.getFragmentManager(), "UploadDialog");
        Log.d(TAG, "Upload diag shown");
    }

    /**
     * Copies file from source to destination.  Purpose is for video to constantly have a new file
     * @param src the original file
     * @param dst the copied file
     * @throws IOException
     */
    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    /**
     * Creates video thumbnail to send to server
     * @param vPath path of the video
     * @return the video thumbnail file
     */
    public static File createVideoThumbnail(String vPath) {
        try {
            File thumb = new File(mActivity.getExternalFilesDir(null), "vthumb.jpeg");
            FileOutputStream out = new FileOutputStream(thumb);
            (ThumbnailUtils.createVideoThumbnail(vPath, MediaStore.Video.Thumbnails.MINI_KIND))
                    .compress(Bitmap.CompressFormat.JPEG, 100, out);
            return thumb;
        } catch(FileNotFoundException e)    {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return mFfmpeg the Ffmpeg
     */
    public static FFmpeg getFfmpeg()    {
        return mFfmpeg;
    }

    /**
     * @param ffmpeg
     */
    public static void setFfmpeg(FFmpeg ffmpeg) {
        mFfmpeg = ffmpeg;
    }

    /**
     * @return activity the host activity
     */
    public static Activity getActivity() {
        return mActivity;
    }

    /**
     * @param mActivity
     */
    public static void setActivity(Activity mActivity) {
        MediaSaver.mActivity = mActivity;
    }
}
