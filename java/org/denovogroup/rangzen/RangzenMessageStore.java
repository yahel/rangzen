package org.denovogroup.rangzen;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RangzenMessageStore extends SQLiteOpenHelper {

    /* Static Access */

    public static final String BROADCAST_STRING = "org.denovo.rangzen.MESSAGES_UPDATED";

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

    /**
     * A helper method for if a priority is within the bounds of acceptable priorities
     *
     * @param priority
     * @return
     */
    public static boolean priorityAcceptable(final double priority) {
        return priority >= 0.0D && priority <= 2.0D;
    }

    /* Sqlite Constants */

    /** The sqlite3 db file name */
    public static final String DATABASE_NAME = "message.db";
    /** The db version */
    public static final int DATABASE_VERSION = 1;
    /** The table in the db file where we store the messages */
    public static final String MSG_TABLE = "messages";

    /** Raw sqlite3 that removes the msg table from our db */
    public static final String DROP_MSG_TABLE = "DROP TABLE IF EXISTS " + MSG_TABLE;

    /** Raw sqlite3 that creates our db */
    public static final String CREATE_MSG_TABLE = "CREATE TABLE " + MSG_TABLE + " (" +
            RangzenMessageColumns._ID + " INTEGER PRIMARY KEY, "
            + RangzenMessageColumns.message + " TEXT, "
            + RangzenMessageColumns.id + " TEXT, "
            + RangzenMessageColumns.priority + " DOUBLE, "
            + RangzenMessageColumns.timeStored + " LONG"
            + ");";

    /** Android sqlite3 queries the message row */
    public static final String WHERE_MESSAGE = RangzenMessageColumns.message + "=?";

    /** Raw sqlite3 that searches for a message CONTAINING %s */
    public static final String SEARCH_FOR_MSG =
            "SELECT * FROM " + MSG_TABLE + " WHERE " + RangzenMessageColumns.message +
                    " LIKE '%%%s%%' COLLATE NOCASE ORDER BY " + RangzenMessageColumns.priority +
                    " COLLATE NOCASE;";

    /** Android sqlite3 that orders our results by priority and then alpha */
    public static final String ORDER_BY_PRIORITY_THEN_CASE =
            RangzenMessageColumns.priority + " DESC, " + RangzenMessageColumns.message + " DESC";

    /** Same as ORDER_BY_PRIORITY_THEN_CASE but also limits the number of results */
    public static final String ORDER_BY_PRIORITY_THEN_CASE_WITH_LIMIT =
            ORDER_BY_PRIORITY_THEN_CASE + " Limit %d";

    /* Start Instance */

    /** Android application context */
    private final Context mContext;
    /** Helper for storing listeners */
    private final SQLiteDatabase mDbConnection;
    /** Write lock, we allow nested calls into write calls */
    private final AtomicInteger mWriteCount;
    /** Helper that notifies listeners when db changes occure */
    private final DbCountMonitor mMonitor;
    /** read lock for sqlite */
    private final Lock mReadLock;
    /** write lock for sqlite */
    private final Lock mWriteLock;

    /**
     * Standard constructor,
     *
     * @param context - a non-null context.
     */
    public RangzenMessageStore(final Context context) {
        this(context, DATABASE_NAME);
    }

    /**
     * Debug constructor, will create a db with the name {@code debugDatabaseName}
     *
     * @param context           - a non-null context.
     * @param debugDatabaseName - name of the data store to create.
     */
    public RangzenMessageStore(final Context context, final String debugDatabaseName) {
        super(context, debugDatabaseName, null, DATABASE_VERSION);

        mContext = context;
        mDbConnection = getWritableDatabase();

        mMonitor = new DbCountMonitor();

        mWriteCount = new AtomicInteger(0);
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

    /**
     * Set a Listener waiting for changes to the datastore. This must be called on the
     * UI thread.
     *
     * @param cursorListener
     */
    public void setDbListener(final DbCountMonitor.DbListener cursorListener) {
        mMonitor.addListener(cursorListener);
    }

    /**
     * Removes a {@link DbCountMonitor.DbListener} if it is currently in the listener list.
     * this must be called on the UI thread.
     *
     * @param dbListener
     */
    public void removeDbListener(final DbCountMonitor.DbListener dbListener) {
        mMonitor.removeListener(dbListener);
    }

    /**
     * Returns a cursor, set prior to the first item, containing all
     * {@link org.denovogroup.rangzen.RangzenMessageStore.RangzenAppMessage} in the db.
     *
     * @return
     */
    public Cursor getAllMessageCursor() {
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

        return cursor;
    }

    /**
     * Get number of messages currently contained in the db
     *
     * @return - number of messages.
     */
    public long getMessageCount() {
        return DatabaseUtils.queryNumEntries(mDbConnection, MSG_TABLE);
    }

    /**
     * Gets a list of {@link org.denovogroup.rangzen.RangzenMessageStore.RangzenAppMessage}
     * currently stored in the db that have the exact message {@code messageQuery}
     *
     * @param messageQuery
     * @return
     */
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

    /**
     * Gets a list of all {@link org.denovogroup.rangzen.RangzenMessageStore.RangzenAppMessage}
     * all stored in the db.
     *
     * @return
     */
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
     * @throws IllegalArgumentException - if {@code kMsgs} is <= 0
     */
    public List<RangzenAppMessage> getKMessages(final int kMsgs) throws IllegalArgumentException {
        if (kMsgs <= 0) {
            throw new IllegalArgumentException(String.format("kMsgs is <= 0 kMsgs:%s", kMsgs));
        }

        final List<RangzenAppMessage> returnList = new ArrayList<>(kMsgs);
        final String query = String.format(ORDER_BY_PRIORITY_THEN_CASE_WITH_LIMIT, kMsgs);
        System.out.println("XXX- " + query);
        Cursor cursor;

        mReadLock.lock();
        try {
            cursor = mDbConnection.query(
                    MSG_TABLE,// Table to query
                    null,     // Return only these two columns
                    null,     // no query string
                    null,     // no query string
                    null,     // group by - default
                    null,     // having   - default
                    query);   // order by
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
        if (!priorityAcceptable(rangzenMessage.mPriority)) {
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
    public boolean updatePriority(final String messageString, final double newPriority) {
        if (messageString == null) {
            throw new IllegalArgumentException("messageString can not be null");
        }
        if (!priorityAcceptable(newPriority)) {
            throw new IllegalArgumentException("rangzenMessage priority out of bounds");
        }


        final ContentValues contentValues = new ContentValues(1);
        contentValues.put(RangzenMessageColumns.priority, newPriority);

        lockForWrite();

        try {
            final int effectedColumns = mDbConnection.update(
                    MSG_TABLE,             // Table to query
                    contentValues,         // update the priority
                    WHERE_MESSAGE,         // get all where the messageString \/
                    new String[]{messageString});// message query string

            return effectedColumns > 0;
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

    /**
     * Deletes ALL messages in the db.
     */
    public void deleteAll() {
        lockForWrite();
        mDbConnection.execSQL("delete from " + MSG_TABLE);
        unlockForWrite();
    }

    /* Private */

    private void broadcastChange() {
        mContext.sendBroadcast(new Intent(BROADCAST_STRING));
    }

    private void lockForWrite() {
        mWriteLock.lock();

        if (mWriteCount.getAndIncrement() == 0) {
            mDbConnection.beginTransaction();
        }
    }

    private void unlockForWrite() {
        if (mWriteCount.decrementAndGet() == 0) {
            mDbConnection.setTransactionSuccessful();
            mDbConnection.endTransaction();
            broadcastChange();
        }

        mWriteLock.unlock();
    }

    /* Db Message object */

    /**
     * Represents a Rangzen Message
     */
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

            final boolean compare = this.mMessage.equals(appMessage.mMessage);

            return compare;
        }

        @Override
        public String toString() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mId:").append(mId.toString());
            stringBuilder.append(" mMessage:").append(mMessage);
            stringBuilder.append(" mPriority:").append(mPriority);
            stringBuilder.append(" mTimeStored:").append(mTimeStored);
            stringBuilder.append(" _id:").append(_id);

            return stringBuilder.toString();
        }
    }


}
