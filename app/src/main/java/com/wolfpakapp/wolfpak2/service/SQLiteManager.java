package com.wolfpakapp.wolfpak2.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.wolfpakapp.wolfpak2.WolfpakSQLiteHelper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLiteManager allows different classes to read and write to an SQLite Database for
 * caching image and video media.
 *
 * @author Roland Fong
 */
public class SQLiteManager extends ServiceManager {

    public static final String TAG = "TAG-SQLiteManager";

    private SQLiteDatabase mSQLiteDatabase;
    private WolfpakSQLiteHelper mSQLiteHelper;

    private String[] mColumns = {
            WolfpakSQLiteHelper.MediaEntry.COLUMN_HANDLE,
            WolfpakSQLiteHelper.MediaEntry.COLUMN_LATITUDE,
            WolfpakSQLiteHelper.MediaEntry.COLUMN_LONGITUDE,
            WolfpakSQLiteHelper.MediaEntry.COLUMN_IS_NSFW,
            WolfpakSQLiteHelper.MediaEntry.COLUMN_IS_IMAGE,
            WolfpakSQLiteHelper.MediaEntry.COLUMN_USER,
            WolfpakSQLiteHelper.MediaEntry.COLUMN_THUMBNAIL,
            WolfpakSQLiteHelper.MediaEntry.COLUMN_MEDIA
    };

    public SQLiteManager(Context context) {
        mSQLiteHelper = new WolfpakSQLiteHelper(context);
        Log.d(TAG, "SQLiteManager has been initialized");
        finishInitialize();
    }

    /**
     * Opens the SQLite database, allowing reading and writing
     *
     * @throws SQLException
     */
    public void open() throws SQLException {
        mSQLiteDatabase = mSQLiteHelper.getWritableDatabase();
    }

    /**
     * Closes the SQLite database
     */
    public void close() {
        mSQLiteHelper.close();
    }

    /**
     * Adds media to table.  Accepts a {@link ContentValues} object containing necessary keys and
     * values to match table column names in {@link com.wolfpakapp.wolfpak2.WolfpakSQLiteHelper.MediaEntry}
     * class. This could be a long running operation and may need to be run in a separate thread
     * or AsyncTask.
     *
     * @param values the encapsulated media
     */
    public void addEntry(ContentValues values) {
        mSQLiteDatabase.insert(WolfpakSQLiteHelper.MediaEntry.TABLE_MEDIA, null, values);
        Log.d(TAG, "Entry added with handle: " + values.get(WolfpakSQLiteHelper.MediaEntry.COLUMN_HANDLE));
    }

    /**
     * A utility method that obtains the handles of all existing entries in database.  For
     * debugging purposes.  This could be a long running operation and may need to be run in
     * a separate thread or AsyncTask.
     *
     * @return list of handles
     */
    public List<String> getMediaHandles() {
        List<String> handles = new ArrayList<>();

        Cursor cursor = mSQLiteDatabase.query(WolfpakSQLiteHelper.MediaEntry.TABLE_MEDIA,
                mColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            handles.add(cursor.getString(0)); // gets the handle
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return handles;
    }

    // TODO add other methods for reading the database
}
