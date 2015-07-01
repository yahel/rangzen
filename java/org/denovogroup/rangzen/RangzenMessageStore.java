package org.denovogroup.rangzen;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.text.TextUtils;

import org.denovogroup.rangzen.RangzenMessageStore.RangzenAppMessage.RangzenMessageColumns;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RangzenMessageStore extends SQLiteOpenHelper {

    private static RangzenMessageStore sRangezenMessageStore;

    /**
     * Control singleton access to the {@link RangzenMessageStore}
     *
     * @param context
     * @return
     */
    public static RangzenMessageStore getInstance(final Context context) {
        if (sRangezenMessageStore == null) {
            synchronized (RangzenMessageStore.class) {
                if (sRangezenMessageStore == null) {
                    sRangezenMessageStore = new RangzenMessageStore(context);
                }
            }
        }

        return sRangezenMessageStore;
    }

    private final SQLiteDatabase mDbConnection;
    private final DbCountMonitor mMonitor;
    private final Lock mReadLock;
    private final Lock mWriteLock;

    public RangzenMessageStore(final Context context) {
        this(context, DATABASE_NAME);
    }

    public RangzenMessageStore(final Context context, final String debugDatabaseName) {
        super(context, debugDatabaseName, null, DATABASE_VERSION);

        mDbConnection = getWritableDatabase();

        mMonitor = new DbCountMonitor();

        final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
        mReadLock = readWriteLock.readLock();
        mWriteLock = readWriteLock.writeLock();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MSG_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Obviously, this will not do if you upgrade
        // in the future, it will to have a data migration.
        db.execSQL(DROP_MSG_TABLE);
        onCreate(db);
    }

    /* Public API */

    public long getMessageCount() {
        return DatabaseUtils.queryNumEntries(mDbConnection, MSG_TABLE);
    }

    public List<RangzenAppMessage> queryMessages(final String messageQuery) {
        final List<RangzenAppMessage> returnList = new ArrayList<>();

        Cursor cursor;

        mReadLock.lock();
        try {
            cursor = mDbConnection.rawQuery(String.format(SEARCH_FOR_MSG, messageQuery), null);
        } finally {
            mReadLock.unlock();
        }


        if (cursor != null) {
            while (cursor.moveToNext()) {
                returnList.add(new RangzenAppMessage(cursor));
            }
        }

        return returnList;
    }

    public List<RangzenAppMessage> getAllMessages() {
        final List<RangzenAppMessage> returnList = new ArrayList<>();

        Cursor cursor;

        mReadLock.lock();
        try {
            cursor = mDbConnection.query(
                    MSG_TABLE,// Table to query
                    null,     // all columns
                    null,     // no query string
                    null,     // no query string
                    null,     // group by - default
                    null,     // having   - default
                    null);    // order by
        } finally {
            mReadLock.unlock();
        }


        if (cursor != null) {
            while (cursor.moveToNext()) {
                returnList.add(new RangzenAppMessage(cursor));
            }
        }

        return returnList;
    }

    /**
     * This method return up to kMsgs sorted by trust score and then in the case of a tie,
     * alphabetically.
     *
     * @param kMsgs a int > 0, the function will return up to kMsgs of RangzenMessage
     * @return up to kMsgs of RangzenMessages
     */
    public List<RangzenAppMessage> getKMessages(final int kMsgs) {
        if (kMsgs <= 0) { return null; }
//        if (kMsgs <= 0) { throw new IllegalArgumentException("kMsgs is <= 0"); }

        final List<RangzenAppMessage> returnList = new ArrayList<>(kMsgs);

        Cursor cursor;

        mReadLock.lock();
        try {
            cursor = mDbConnection.query(
                    MSG_TABLE,// Table to query
                    null,// Return only these two columns
                    null,     // no query string
                    null,     // no query string
                    null,     // group by - default
                    null,     // having   - default
                    String.format(ORDER_BY_PRIORITY_THEN_CASE_WITH_LIMIT, kMsgs));// order by
        } finally {
            mReadLock.unlock();
        }


        if (cursor != null) {
            while (cursor.moveToNext()) {
                returnList.add(new RangzenAppMessage(cursor));
            }
        }

        return returnList;
    }

    /**
     * Creates a new row inserting this message, any other message with the same
     * message body IS REMOVED.
     *
     * @param message
     * @param priority
     */
    public void insertMessage(final String message, final double priority) {
        this.insertMessage(new RangzenAppMessage(message, priority));
    }

    /**
     * Creates a new row inserting this message, any other message with the same
     * message body IS REMOVED.
     *
     * @param rangzenMessage
     */
    public void insertMessage(final RangzenAppMessage rangzenMessage) {
        if (rangzenMessage == null) {
            throw new IllegalArgumentException("rangzenMessage can not be null");
        }
        if (rangzenMessage.mPriority < 0D || rangzenMessage.mPriority > 1D) {
            throw new IllegalArgumentException("rangzenMessage priority out of bounds");
        }
        if (TextUtils.isEmpty(rangzenMessage.mMessage)) {
            throw new IllegalArgumentException("rangzenMessage body is empty");
        }
        if (rangzenMessage.mMessage.length() > 140) {
            throw new IllegalArgumentException("rangzenMessage body is more than 140 characters!");
        }

        final ContentValues contentValues = rangzenMessage.dehydrate();

        lockForWrite();

        try {
            // Delete any rangzenMessages with the same body text
            deleteMessage(rangzenMessage.mMessage);
            // Insert the new message into our DB
            mDbConnection.insert(MSG_TABLE, null, contentValues);
        } finally {
            unlockForWrite();
        }
    }

    /**
     * Update the message priority to {@code newPriority} where the text is {@code messageString}
     *
     * @param messageString
     * @param newPriority
     */
    public void updatePriority(final String messageString, final double newPriority) {
        if (messageString == null) {
            throw new IllegalArgumentException("messageString can not be null");
        }
        if (newPriority < 0D || newPriority > 1D) {
            throw new IllegalArgumentException("rangzenMessage priority out of bounds");
        }


        final ContentValues contentValues = new ContentValues(1);
        contentValues.put(RangzenMessageColumns.priority, newPriority);

        lockForWrite();

        try {
            mDbConnection.update(
                    MSG_TABLE,             // Table to query
                    contentValues,         // update the priority
                    WHERE_MESSAGE,         // get all where the messageString \/
                    new String[]{messageString});// message query string
        } finally {
            unlockForWrite();
        }
    }

    /**
     * Looks up a {@link RangzenMessageStore.RangzenAppMessage}
     * by it's {@link RangzenMessageStore.RangzenAppMessage#mMessage}
     *
     * @param messageString
     * @return null if we don't find a message,
     * otherwise the referenced {@link RangzenMessageStore.RangzenAppMessage}
     */
    public RangzenAppMessage lookupByMessage(final String messageString) {
        if (messageString == null) {
            throw new IllegalArgumentException("messageString can not be null");
        }

        Cursor cursor;

        mReadLock.lock();
        try {
            cursor = mDbConnection.query(
                    MSG_TABLE,             // Table to query
                    null,                  // Return all columns
                    WHERE_MESSAGE,         // get all where the messageString ==
                    new String[]{messageString}, // message query string
                    null,                  // group by - defualt
                    null,                  // having   - defualt
                    null);                 // order by - default
        } finally {
            mReadLock.unlock();
        }

        if (cursor != null && cursor.moveToNext()) {
            return new RangzenAppMessage(cursor);
        }

        return null;
    }

    /**
     * @param messageString
     */
    public void deleteMessage(final String messageString) {
        lockForWrite();
        try {
            mDbConnection.delete(MSG_TABLE, WHERE_MESSAGE, new String[]{messageString});
        } finally {
            unlockForWrite();
        }
    }

    /* Private */

    private void lockForWrite() {
        mWriteLock.lock();

        if (mMonitor.willNotifyListeners()) {
            mDbConnection.beginTransaction();
        }
    }

    private void unlockForWrite() {
        if (mMonitor.notifyListeners()) {
            mDbConnection.setTransactionSuccessful();
            mDbConnection.endTransaction();
        }

        mWriteLock.unlock();
    }

    /* Db Message object */

    public static class RangzenAppMessage {

        /** The message body */
        public final String mMessage;
        /** Rangzen Priority, how much trust we put in this message */
        public final Double mPriority;
        /** Timestampe of when we stored this object */
        public final long mTimeStored;
        /** A local Id for identifying this message */
        public final UUID mId;
        /** Needed for some Android utility functions */
        public final int _id;

        /**
         * Used when we receive a message over the network,
         * we can then serialize it for local storage
         *
         * @param rangzenMessage - non-null Rangzen message
         */
        public RangzenAppMessage(final RangzenMessage rangzenMessage) {
            this(rangzenMessage.text, rangzenMessage.priority);
        }

        public RangzenAppMessage(final String message, final double priority) {
            mMessage = message;
            mPriority = priority;
            mTimeStored = System.currentTimeMillis();
            mId = UUID.randomUUID();
            _id = -1;
        }


        /**
         * Empty constructor, object is invalid.
         */
        public RangzenAppMessage() {
            mMessage = null;
            mPriority = -1D;
            mTimeStored = -1;
            mId = null;
            _id = -1;
        }

        /**
         * For inflating ourselves from the datastore.
         *
         * @param cursor - initialized and non-empty cursor
         */
        public RangzenAppMessage(final Cursor cursor) {
            mMessage = cursor.getString(cursor.getColumnIndex(RangzenMessageColumns.message));
            mPriority = cursor.getDouble(cursor.getColumnIndex(RangzenMessageColumns.priority));
            mTimeStored = cursor.getLong(cursor.getColumnIndex(RangzenMessageColumns.timeStored));
            _id = cursor.getInt(cursor.getColumnIndex(RangzenMessageColumns._ID));
            String uuidString = cursor.getString(cursor.getColumnIndex(RangzenMessageColumns.id));
            UUID uuid = null;
            if (!TextUtils.isEmpty(uuidString)) {
                try {
                    uuid = UUID.fromString(uuidString);
                } catch (IllegalArgumentException e) { }
            }

            mId = uuid;
        }

        /**
         * Dehydrates the object for storage into the sqlite db
         *
         * @return Contentvalues to be stored in the datastore.
         */
        public ContentValues dehydrate() {
            final ContentValues values = new ContentValues();

            values.put(RangzenMessageColumns.message, mMessage);
            values.put(RangzenMessageColumns.priority, mPriority);
            values.put(RangzenMessageColumns.timeStored, mTimeStored);
            values.put(RangzenMessageColumns.id, (mId != null ? mId.toString() : ""));

            return values;
        }

        /**
         * Class representing our column names in the sqlite datastore.
         */
        public static class RangzenMessageColumns implements BaseColumns {
            public static String message = "rangzen_message";
            public static String priority = "rangzen_priority";
            public static String timeStored = "rangzen_timeStored";
            public static String id = "rangzen_id";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }

            if (o == null || !(o instanceof RangzenAppMessage) || this.mMessage == null) {
                return false;
            }

            final RangzenAppMessage appMessage = (RangzenAppMessage) o;
            if (appMessage.mMessage == null) { return false; }

            return this.mMessage.equals(appMessage.mMessage);
        }
    }

    /* Sqlite Constants */

    public static final String DATABASE_NAME = "message.db";
    public static final int DATABASE_VERSION = 1;
    public static final String MSG_TABLE = "messages";

    public static final String DROP_MSG_TABLE = "DROP TABLE IF EXISTS " + MSG_TABLE;

    public static final String CREATE_MSG_TABLE = "CREATE TABLE " + MSG_TABLE + " (" +
            RangzenMessageColumns._ID + " INTEGER PRIMARY KEY, "
            + RangzenMessageColumns.message + " TEXT, "
            + RangzenMessageColumns.id + " TEXT, "
            + RangzenMessageColumns.priority + " DOUBLE, "
            + RangzenMessageColumns.timeStored + " LONG"
            + ");";

    public static final String WHERE_MESSAGE = RangzenMessageColumns.message + "=?";

    public static final String SEARCH_FOR_MSG =
            "SELECT * FROM " + MSG_TABLE + " WHERE " + RangzenMessageColumns.message +
                    " LIKE '%%%s%%' COLLATE NOCASE ORDER BY " + RangzenMessageColumns.priority +
                    " COLLATE NOCASE;";

    public static final String ORDER_BY_PRIORITY_THEN_CASE =
            RangzenMessageColumns.priority + " ASC, " + RangzenMessageColumns.message + "ASC";
    public static final String ORDER_BY_PRIORITY_THEN_CASE_WITH_LIMIT =
            ORDER_BY_PRIORITY_THEN_CASE + " Limit = %d";
}
