package com.wolfpakapp.wolfpak2.camera.editor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.camera2.CameraCharacteristics;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.wolfpakapp.wolfpak2.camera.preview.CameraLayout;
import com.wolfpakapp.wolfpak2.service.LocationProvider;

import org.apache.http.Header;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
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

    /**
     * A hashmap of all the request parameters
     */
    private static HashMap mMap = new HashMap(8);
    /**
     * An key array of parameters for the server
     */
    public static final String[] keys =
            { "handle", "latitude", "longitude", "is_nsfw", "is_image","user", "thumbnail", "media" };
    /**
     * Request parameters for server upload
     */
    private static RequestParams params;

    private static FFmpeg mFfmpeg;
    private static Activity mActivity;

    public static final String VIDEO_PATH = "Video Path";
    public static final String OVERLAY_PATH = "Overlay Path";
    public static final String OUTPUT_PATH = "Output Path";
    public static final String IS_UPLOADING = "Is Uploading";

    private static ProgressDialog progressDialog;

    public interface MediaSaverListener {
        public void onDownloadCompleted();
        public void onUploadCompleted();
    };

    /**
     * Notifies when upload or download completes
     */
    private static MediaSaverListener mMediaSaverListener;

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
        progressDialog = new ProgressDialog(mActivity);
        // use AsyncTask to start a progress dialog
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog.setTitle("Saving...");
                progressDialog.setMessage("Please Wait.");
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(true);
                progressDialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                // save image
                try {
                    saveImagetoFile(image, overlay);
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
                progressDialog.dismiss();
                listener.onDownloadCompleted();
            }
        };
        task.execute((Void[])null);
    }

    /**
     * Saves image with overlay to file as image media
     * @param image the background image
     * @param overlay the foreground overlay
     * @return path the filepath
     * @throws IOException
     */
    private static String saveImagetoFile(Bitmap image, Bitmap overlay) throws IOException   {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File imagefile = File.createTempFile(timeStamp, ".jpeg",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
        String filePath = imagefile.getAbsolutePath();

        // write data to file system
        FileOutputStream output = new FileOutputStream(imagefile);

        // combines overlay and textureview
        Canvas c = new Canvas(image);
        c.drawBitmap(overlay, 0, 0, null);
        image.compress(Bitmap.CompressFormat.JPEG, 75, output);

        // add metadata
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);
        // insert image
        mActivity.getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        // stores the image with other image media (accessible through Files > Images)
        MediaStore.Images.Media.insertImage(mActivity.getContentResolver(),
                filePath, imagefile.getName(), "No Description");
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
    public static void uploadImage(final MediaSaverListener listener, Bitmap image, Bitmap overlay)    {

        File file = null;
        try {
            file = new File(saveImagetoFile(image, overlay));
        } catch(IOException e)  {
            e.printStackTrace();
        }

        // create upload parameters
        generateUploadParams(listener, file, true);
        // when user finishes input, upload will begin
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

    public static void downloadVideo(final MediaSaverListener listener, final String videoPath,
                                     final Bitmap overlay)  {
        progressDialog = new ProgressDialog(mActivity);
        // use AsyncTask to start a progress dialog
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog.setTitle("Saving...");
                progressDialog.setMessage("Please Wait.");
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(true);
                progressDialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    saveVideotoFile(videoPath, overlay, false);
                    // buy some time
                    stall(listener, false);
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
                progressDialog.dismiss();
                listener.onDownloadCompleted();
            }
        };
        task.execute((Void[]) null);
    }

    /**
     * Saves video to file system in movies directory
     * @param videoPath path of original video
     * @param overlay the foreground overlay
     * @param isUploading whether an upload to server is expected
     * @throws IOException
     */
    private static void saveVideotoFile(String videoPath, Bitmap overlay,
                                        boolean isUploading) throws IOException {
        // make a copy of the video; ensures it will not be overwritten
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File videoFile = new File(mActivity.getExternalFilesDir(null), timeStamp + ".mp4");
        copy(new File(videoPath), videoFile);

        // create and save overlay
        File tempImgFile = new File(mActivity.getExternalFilesDir(null), "overlay.png");
        FileOutputStream output = new FileOutputStream(tempImgFile);
        // blit overlay onto texture
        Bitmap finalImage = Bitmap.createBitmap(overlay);
        Matrix matrix = new Matrix(); // used to rotate the overlay 90 degrees because god knows why...
        matrix.postRotate(-90);
        Bitmap resizedBitmap = Bitmap.createBitmap(
                finalImage, 0, 0, finalImage.getWidth(), finalImage.getHeight(), matrix, true);
        // compresses whatever textureview and overlay have into output
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 60, output);

        // create output file
        String outputPath = "MP4_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES); // is there a standard video directory?
        File video= File.createTempFile(outputPath, ".mp4", storageDir); // TODO don't use tempfile?

        // start intent to service to handle this in background
        Intent intent = new Intent(mActivity, VideoSavingService.class);
        intent.putExtra(VIDEO_PATH, videoFile.getCanonicalPath());
        intent.putExtra(OVERLAY_PATH, tempImgFile.getCanonicalPath());
        intent.putExtra(OUTPUT_PATH, video.getCanonicalPath());
        intent.putExtra(IS_UPLOADING, isUploading);
        mActivity.startService(intent);
    }

    /**
     * Upload video to server
     * @param listener the mediasaverlistener
     * @param videoPath the path of the original video
     * @param overlay the foreground overlay
     */
    public static void uploadVideo(final MediaSaverListener listener, String videoPath,
                                   Bitmap overlay)   {
        generateUploadParams(listener, null, false);
        try {
            saveVideotoFile(videoPath, overlay, true);
        } catch(IOException e)  {
            e.printStackTrace();
        }
    }

    /**
     * Waits 5 seconds.  For stalling progress dialog to buy some time
     */
    private static void stall(final MediaSaverListener listener, final boolean isUpload) {
        (new Thread(new Runnable()  {
            @Override
            public void run() {
                // just wait 5s to buy some time
                try {
                    Thread.sleep(5000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
                if(isUpload) {
                    progressDialog.dismiss();
                    listener.onUploadCompleted();
                }
            }
        })).start();
    }

    /**
     * Upload method specifically to be called upon video saving completion.
     * Complets the rest of the parameters (media and thumbnail) and initiates uploading process
     * @param file
     */
    public static void upload(final File file, final boolean isImage)  {
        // this is specifically for video!!
        if(isImage)   {
            Log.e(TAG, "Image attempted wrong upload call!");
        } else  {
            // add the now saved video file (and thumbnail)
            try {
                params.put("media", file);
                params.put("thumbnail", createVideoThumbnail(file.getAbsolutePath()));
            } catch(FileNotFoundException e)    {
                e.printStackTrace();
            }
        }
        // now do regular upload
        upload((MediaSaverListener) null, isImage);
    }

    /**
     * Reusable function that sends created file (image or video) to server.
     * Generates parameters and starts AsyncHttpClient.
     * @param listener the MediaSaverListener
     */
    private static void upload(final MediaSaverListener listener, final boolean isImage)   {
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
                        listener.onUploadCompleted();
                        progressDialog.dismiss();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.e(TAG, "Upload Failure " + statusCode);
                    // only call if this is image, video already called
                    if(isImage) {
                        listener.onUploadCompleted();
                        progressDialog.dismiss();
                    }
                }
            });
        } else { // alert user if connection fails
            Log.e(TAG, "Couldn't connect to network");
            Toast.makeText(mActivity, "Upload failed: Couldn't connect to network", Toast.LENGTH_SHORT);
        }
    }

    /**
     * Reusable function to generate RequestParameters for upload.
     * Displays an upload dialog for user defined parameters.
     * Begins the upload after user inputs requested parameters.
     * @param file the file to be sent
     */
    public static void generateUploadParams(final MediaSaverListener listener,
                                            final File file, final boolean isImage)   {

        UploadDialog uploadDialog = new UploadDialog();
        uploadDialog.setUploadDialogListener(new UploadDialog.UploadDialogListener() {
            @Override
            public void onDialogPositiveClick(UploadDialog dialog) {
                // start progress dialog
                progressDialog = ProgressDialog.show(mActivity, "Please Wait...", "Sending...", true);
                if(!isImage)
                    stall(listener, true); // buy some time for video saving
                // initialize server params here
                mMap.put("handle", dialog.getHandle());
                mMap.put("is_nsfw", dialog.isNsfw() ? "true" : "false");
                mMap.put("is_image", PictureEditorLayout.isImage() ? "true" : "false");
                mMap.put("user", "temp_test_id");
                mMap.put("latitude", LocationProvider.getLastLocation().getLatitude());
                mMap.put("longitude", LocationProvider.getLastLocation().getLongitude());

                params = new RequestParams();

                for(String key : keys)  {
                    if(key != "media" && key != "thumbnail") {
                        params.put(key, mMap.get(key));
                    }
                }

                try {
                    /*if(!PictureEditorLayout.isImage()) {
                        params.put("thumbnail",
                                createVideoThumbnail(file.getAbsolutePath(), activity));
                    } else { // NOTE we are explictly not putting file in if it's video b/c not saved yet!
                        params.put("media", file);
                        Log.d(TAG, "PUT: media " + file.toString());
                    }*/
                    if(isImage)   {
                        params.put("media", file); // put image BUT NOT VIDEO YET b/c not done saving
                    }
                } catch(FileNotFoundException e)    {
                    e.printStackTrace();
                }

                // upload file (but don't do it yet if it's video!!
                if(isImage)
                    upload(listener, isImage);
            }

            @Override
            public void onDialogNegativeClick(UploadDialog dialog) {
            }
        });
        uploadDialog.show(mActivity.getFragmentManager(), "UploadDialog");
    }

    /**
     * Creates video thumbnail to send to server
     * @param vPath path of the video
     * @return the video thumbnail file
     */
    private static File createVideoThumbnail(String vPath) {
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
     * Returns upload request params for video saving service access
     * @return params the request parameters
     */
    public static RequestParams getRequestParams()  {
        return params;
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
