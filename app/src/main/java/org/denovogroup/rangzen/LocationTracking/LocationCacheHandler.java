package org.denovogroup.rangzen.LocationTracking;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Liran on 8/31/2015.
 *
 * This class is responsible for maintaining local SQLite data base as a cache for locations being sampled from
 * the TrackingService, it is meant to be used as a singleton.
 *
 */
public class LocationCacheHandler extends SQLiteOpenHelper{

    private static LocationCacheHandler instance;
    private final static String LOG_TAG = "LocationCacheHandler";
    private final static int DATABASE_VERSION = 1;
    private final static String DATABASE_NAME = "locationCache.db";

    private final static String TABLE_NAME = "LOCATION_CACHE";
    public final static String TIMESTAMP_COL = "TIMESTAMP";
    public final static String LATITUDE_COL = "LATITUDE";
    public final static String LONGITUDE_COL = "LONGITUDE";

    public static LocationCacheHandler getInstance(Context context){
        if(instance == null){
            instance = new LocationCacheHandler(context);
        }
        return instance;
    }

    public LocationCacheHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTableIfNoteExist(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Since this database is a cache, just discard everything on update
        dropTable(db);
        //call oncreate again to ensure tables are being recreated after being dropped
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    private void createTableIfNoteExist(SQLiteDatabase db){
        if(db != null) {
            String sqlCommand = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" +
                    TIMESTAMP_COL + " INT PRIMARY KEY NOT NULL," +
                    LATITUDE_COL + " REAL NOT NULL," +
                    LONGITUDE_COL + " REAL NOT NULL" +
                    ");";
            db.execSQL(sqlCommand);
        }
    }

    /** Drops this handlers table, make sure to recreate a table before requesting
     * any more cache related actions.
     *
     * @param db the database from which to drop the table
     */
    private void dropTable(SQLiteDatabase db){
        if(db != null) {
            String sqlCommand = "DROP TABLE IF EXISTS " + TABLE_NAME+";";
            db.execSQL(sqlCommand);
        }
    }

    /** Adds an item to the cache
     *
     * @param trackedLocation to be cached
     */
    public void insertLocation(TrackedLocation trackedLocation){
        if(trackedLocation != null){
            ContentValues content = new ContentValues();
            content.put(LATITUDE_COL, trackedLocation.latitude);
            content.put(LONGITUDE_COL, trackedLocation.longitude);
            content.put(TIMESTAMP_COL, trackedLocation.timestamp);

            SQLiteDatabase db = getWritableDatabase();
            if(db != null){
                db.insert(TABLE_NAME, null, content);
                //Log.d(LOG_TAG, "saved to cache");
            }
        }
    }

    /** Removes a single item from the cache based on the timestamp value
     * of the supplied TrackedLocation
     *
     * @param trackedLocation item to be removed - system will look for an item in the cache
     *                        with the same timestamp value and remove it.
     */
    public void removeLocation(TrackedLocation trackedLocation){
        if(trackedLocation != null) {
            String sqlCommand = "DELETE FROM " + TABLE_NAME + " WHERE " +
                    TIMESTAMP_COL+" = "+trackedLocation.timestamp+";";

            SQLiteDatabase db = getWritableDatabase();
            if(db != null){
                db.execSQL(sqlCommand);
                //Log.d(LOG_TAG, "location removed from cache");
            }
        }

    }

    /** a helper class used to get this helpers database cursor
     *
     * @return cursor object with all the rows of this database
     */
    public Cursor getCursor(){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            String sqlQuery = "SELECT * FROM "+TABLE_NAME+" ORDER BY "+TIMESTAMP_COL+" DESC;";
            return db.rawQuery(sqlQuery, null);
        }
        return null;
    }

    /** calculating the amount of items in the cache
     *
     * @return the amount of items in the cache database
     */
    public int getCacheSize(){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            String sqlQuery = "SELECT * FROM "+TABLE_NAME+";";
            Cursor cursor = db.rawQuery(sqlQuery, null);
            int rowCount = cursor.getCount();
            cursor.close();
            return rowCount;
        }
        return 0;
    }

    /** a helper class used during development to wipe the database without the need to upgrade
     * database versions
     */
    public void purgeCache(){
        dropTable(getWritableDatabase());
        onCreate(getWritableDatabase());
    }
}