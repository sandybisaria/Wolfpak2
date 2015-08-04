package com.wolfpakapp.wolfpak2.camera.editor;

import android.app.Service;
import android.content.Intent;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.wolfpakapp.wolfpak2.camera.preview.CameraLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class VideoSavingService extends Service {

    private static final String TAG = "TAG-VideoSavingService";

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    /**
     * Requests by message mapped to an intent.  Allows processing of multiple video save requests
     */
    private HashMap<Message, Intent> requests;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // needed to be able to pass to ffmpeg callback
            final Message msg2 = msg;

            // unbundle intent
            Intent intent = requests.get(msg);
            if(intent != null) {
                final String videoPath = (String) intent.getExtras().get(MediaSaver.VIDEO_PATH);
                final String overlayPath = (String) intent.getExtras().get(MediaSaver.OVERLAY_PATH);
                final String outputPath = (String) intent.getExtras().get(MediaSaver.OUTPUT_PATH);
                final boolean isUploading = (boolean) intent.getExtras().get(MediaSaver.IS_UPLOADING);

                // Generate FFmpeg command: overlays image (overlay), rotates 90 degrees to
                // vertical orientation (transpose) and compresses video (qscale)
                String cmd, transpose, audio, speed;
                transpose = (CameraLayout.getFace() ==
                        CameraCharacteristics.LENS_FACING_FRONT) ? "3" : "1"; // 1=rotate, 3=rotate/flip
                audio = (CameraLayout.isSound()) ? "-map 0:a " : ""; // audio map
                speed = isUploading ? "veryfast" : "ultrafast"; // server needs more compression so go slow
                // actual command
                cmd = "-y -i " + videoPath + " -i " + overlayPath +
                        " -strict -2 -qp 29 -filter_complex" +
                        " [0:v][1:v]overlay=0:0,transpose=" + transpose +
                        "[out] -map [out] " + audio + "-codec:v libx264 -preset " + speed +
                        " -codec:a copy " + outputPath;
                Log.d(TAG, "COMMAND: " + cmd);

                // execute command
                try {
                    MediaSaver.getFfmpeg().execute(cmd, new ExecuteBinaryResponseHandler() {
                        @Override
                        public void onStart() {}
                        @Override
                        public void onProgress(String message) {
                            //Log.d(TAG, "Progress: " + message);
                        }
                        @Override
                        public void onFailure(String message) {
                            Log.d(TAG, "Failure: " + message);
                        }
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "Success: " + message);
                        }
                        @Override
                        public void onFinish() {
                            // destroy the input temp files
                            (new File(videoPath)).delete();
                            (new File(overlayPath)).delete();
                            if(isUploading) {
                                // continue and send it to the server
                                MediaSaver.upload(new File(outputPath), false);
                            }

                            // Stop the service using the startId, so that we don't stop
                            // the service in the middle of handling another job
                            stopSelf(msg2.arg1);
                        }
                    });
                } catch (FFmpegCommandAlreadyRunningException e) {
                    // Handle if FFmpeg is already running
                }
            } else  { // intent was null; possibly sent in error
                stopSelf(msg.arg1); // stop the service prevent crashing
            }
        }
    }

    @Override
    public void onCreate() {
        requests = new HashMap<Message, Intent>();
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting new command id: " + startId);
        // Toast.makeText(this, "Video Save Starting", Toast.LENGTH_SHORT).show();
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        // add intent to list of pending save requests
        requests.put(msg, intent);
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        // Toast.makeText(this, "Video Save Done", Toast.LENGTH_SHORT).show();
    }
}
