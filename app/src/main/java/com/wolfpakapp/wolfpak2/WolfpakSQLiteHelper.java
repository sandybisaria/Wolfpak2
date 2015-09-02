package com.wolfpakapp.wolfpak2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * WolfpakSQLiteHelper holds database attributes and manages the database.
 *
 * @author Roland Fong
 */
public class WolfpakSQLiteHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "wolfpakmedia.db";
    public static final int DATABASE_VERSION = 1;

    /**
     * Contains table information, including table and column names
     */
    public static abstract class MediaEntry implements BaseColumns {

        public static final String TABLE_MEDIA = "table_media";

        public static final String COLUMN_HANDLE = "handle";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGITUDE = "longitude";
        public static final String COLUMN_IS_NSFW = "is_nsfw";
        public static final String COLUMN_IS_IMAGE = "is_image";
        public static final String COLUMN_USER = "user";
        public static final String COLUMN_THUMBNAIL = "thumbnail";
        public static final String COLUMN_MEDIA = "media";

    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + MediaEntry.TABLE_MEDIA + " (" +
                    MediaEntry._ID + " INTEGER PRIMARY KEY," +
                    MediaEntry.COLUMN_HANDLE + TEXT_TYPE + COMMA_SEP +
                    MediaEntry.COLUMN_LATITUDE + TEXT_TYPE + COMMA_SEP +
                    MediaEntry.COLUMN_LONGITUDE + TEXT_TYPE + COMMA_SEP +
                    MediaEntry.COLUMN_IS_NSFW + TEXT_TYPE + COMMA_SEP +
                    MediaEntry.COLUMN_IS_IMAGE + TEXT_TYPE + COMMA_SEP +
                    MediaEntry.COLUMN_USER + TEXT_TYPE + COMMA_SEP +
                    MediaEntry.COLUMN_THUMBNAIL + TEXT_TYPE + COMMA_SEP +
                    MediaEntry.COLUMN_MEDIA + TEXT_TYPE +
                    " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + MediaEntry.TABLE_MEDIA;

    public WolfpakSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(WolfpakSQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

}
