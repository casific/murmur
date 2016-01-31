/*
* Copyright (c) 2016, De Novo Group
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice,
* this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright notice,
* this list of conditions and the following disclaimer in the documentation
* and/or other materials provided with the distribution.
*
* 3. Neither the name of the copyright holder nor the names of its
* contributors may be used to endorse or promote products derived from this
* software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRES S OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package org.denovogroup.murmur.backend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import org.apache.log4j.Logger;
import org.denovogroup.murmur.objects.MurmurMessage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Storage for Murmur messages that uses StorageBase underneath. If
 * instantiated as such, automatically encrypts and decrypts data before storing
 * in Android.
 */
public class MessageStore extends SQLiteOpenHelper {

    public static final String NEW_MESSAGE = "new message";

    private static String storeVersion;

    private static MessageStore instance;
    private static final String TAG = "MessageStore";
    private static final Logger log = Logger.getLogger(TAG);
    //readable true/false operators since SQLite does not support boolean values
    public static final int TRUE = 1;
    public static final int FALSE = 0;

    //messages properties
    private static final double MIN_TRUST = 0.01f;
    private static final double MAX_PRIORITY_VALUE = 1.0f;
    private static final int MAX_MESSAGE_SIZE = 140;
    private static final double DEFAULT_PRIORITY = 0;

    private static final String DATABASE_NAME = "MessageStore.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE = "Messages";
    public static final String COL_ROWID = "_id";
    public static final String COL_MESSAGE_ID = "messageId";
    public static final String COL_MESSAGE = "Message";
    public static final String COL_TRUST = "Connection_score";
    public static final String COL_LIKES = "Endorsements";
    public static final String COL_LIKED = "Endorsed";
    public static final String COL_PSEUDONYM = "Nickname";
    public static final String COL_TIMESTAMP = "Timestamp";
    private static final String COL_DELETED = "deleted";
    public static final String COL_READ = "read";
    public static final String COL_EXPIRE = "expire";
    public static final String COL_LATLONG = "Location";
    public static final String COL_BIGPARENT = "bigparent";
    public static final String COL_PARENT = "parent";
    public static final String COL_FAVIRITE = "favorited";
    public static final String COL_CHECKED = "checked";
    public static final String COL_EXCHANGE = "exchange";
    public static final String COL_MIN_CONTACTS_FOR_HOP = "Restricted";
    public static final String COL_HOP = "hop";

    private static final String[] defaultSort = new String[]{COL_DELETED,COL_READ};

    private String sortOption;

    /** Get the current instance of MessageStore and create one if necessary.
     * Implemented as a singleton */
    public synchronized static MessageStore getInstance(Context context){
        if(instance == null && context != null){
            instance = new MessageStore(context);
            instance.setSortOption(new String[]{COL_ROWID}, false);
        }
        return instance;
    }

    /** Get the current instance of MessageStore or null if none already created */
    public synchronized static MessageStore getInstance(){
        return getInstance(null);
    }

    /** private constructor for forcing singleton pattern for MessageStore */
    private MessageStore(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        instance = this;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + COL_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_MESSAGE_ID + " TEXT NOT NULL,"
                + COL_BIGPARENT + " TEXT,"
                + COL_PARENT + " TEXT,"
                + COL_MESSAGE + " VARCHAR(" + MAX_MESSAGE_SIZE + ") NOT NULL,"
                + COL_TIMESTAMP + " INTEGER NOT NULL,"
                + COL_EXPIRE + " INTEGER NOT NULL,"
                + COL_TRUST + " REAL NOT NULL DEFAULT " + MIN_TRUST + ","
                + COL_LIKES + " INT NOT NULL DEFAULT " + DEFAULT_PRIORITY + ","
                + COL_HOP + " INT NOT NULL DEFAULT " + 0 + ","
                + COL_MIN_CONTACTS_FOR_HOP + " INT NOT NULL DEFAULT " + 0 + ","
                + COL_PSEUDONYM + " VARCHAR(255) NOT NULL,"
                + COL_LATLONG + " TEXT,"
                + COL_EXCHANGE + " TEXT,"
                + COL_LIKED + " BOOLEAN DEFAULT " + FALSE + " NOT NULL CHECK(" + COL_LIKED + " IN(" + TRUE + "," + FALSE + ")),"
                + COL_DELETED + " BOOLEAN DEFAULT " + FALSE + " NOT NULL CHECK(" + COL_DELETED + " IN(" + TRUE + "," + FALSE + ")),"
                + COL_FAVIRITE + " BOOLEAN DEFAULT " + FALSE + " NOT NULL CHECK(" + COL_FAVIRITE + " IN(" + TRUE + "," + FALSE + ")),"
                + COL_CHECKED + " BOOLEAN DEFAULT " + FALSE + " NOT NULL CHECK(" + COL_CHECKED + " IN(" + TRUE + "," + FALSE + ")),"
                + COL_READ + " BOOLEAN DEFAULT " + FALSE + " NOT NULL CHECK(" + COL_READ + " IN(" + TRUE + "," + FALSE + "))"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*recreate table on upgrade, this should be better implemented once final data base structure
          is reached*/
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Check that the given trust value is in range.
     * @throws IllegalArgumentException if value is outside of range
     */
    private static void checkTrust(double priority) throws IllegalArgumentException{
        if (priority < MIN_TRUST || priority > MAX_PRIORITY_VALUE) {
            throw new IllegalArgumentException("Priority " + priority
                    + " is outside valid range of ["+ MIN_TRUST +","+MAX_PRIORITY_VALUE+"]");
        }
    }

    /**
     * Check check that the given trust value is in range and return nearest limit if not.
     */
    private static double streamlineTrust(double priority){
        if (priority < MIN_TRUST){
            return MIN_TRUST;

        } else if(priority > MAX_PRIORITY_VALUE) {
            return MAX_PRIORITY_VALUE;
        }else{
            return priority;
        }
    }

    /** convert cursor data returned from SQL queries into Message objects that can be returned to
     * query supplier. This implementation does not close the supplied cursor when done
     * @param cursor Cursor data returned from SQLite database
     * @return list of Message items contained by the cursor or an empty list if cursor was empty
     */
    private List<MurmurMessage> convertToMessages(Cursor cursor){

        List<MurmurMessage> messages = new ArrayList<>();
        cursor.moveToFirst();

        int messageIdColIndex = cursor.getColumnIndex(COL_MESSAGE_ID);
        int trustColIndex = cursor.getColumnIndex(COL_TRUST);
        int priorityColIndex = cursor.getColumnIndex(COL_LIKES);
        int messageColIndex = cursor.getColumnIndex(COL_MESSAGE);
        int pseudonymColIndex = cursor.getColumnIndex(COL_PSEUDONYM);
        int timestampColIndex = cursor.getColumnIndex(COL_TIMESTAMP);
        int latlongColIndex = cursor.getColumnIndex(COL_LATLONG);
        int timeboundColIndex = cursor.getColumnIndex(COL_EXPIRE);
        int parentColIndex = cursor.getColumnIndex(COL_PARENT);
        int bigparentColIndex = cursor.getColumnIndex(COL_BIGPARENT);
        int hopColIndex = cursor.getColumnIndex(COL_HOP);
        int hopContactsColIndex = cursor.getColumnIndex(COL_MIN_CONTACTS_FOR_HOP);

        if (cursor.getCount() > 0) {
            while (!cursor.isAfterLast()){
                messages.add(new MurmurMessage(
                        cursor.getString(messageIdColIndex),
                        cursor.getString(messageColIndex),
                        cursor.getDouble(trustColIndex),
                        cursor.getInt(priorityColIndex),
                        cursor.getString(pseudonymColIndex),
                        cursor.getLong(timestampColIndex),
                        cursor.getString(latlongColIndex),
                        cursor.getLong(timeboundColIndex),
                        cursor.getString(parentColIndex),
                        cursor.getInt(hopColIndex),
                        cursor.getString(bigparentColIndex),
                        cursor.getInt(hopContactsColIndex)
                ));
                cursor.moveToNext();
            }
        }

        return messages;
    }

    /** Return a cursor pointing to messages sorted by according their priority and deleted state
     * @param getDeleted whether or not results should include deleted items
     * @param getReplies whether or not results should include items which are replies on other message
     * @param limit Maximum number of items to return or -1 for unlimited
     * @return Cursor with Message items based on database items matching conditions
     */
    public Cursor getMessagesCursor(boolean getDeleted,boolean getReplies, int limit){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null) {
            String query = "SELECT * FROM " + TABLE
                    + " WHERE "
                        + (!getReplies ? "("+COL_BIGPARENT+ " IS NULL OR "+COL_BIGPARENT+" NOT IN (SELECT "+COL_MESSAGE_ID+" FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+") AND "+COL_PARENT+" NOT IN (SELECT "+COL_MESSAGE_ID+" FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+ "))" : "")
                        + " "+(!getDeleted ? ((!getReplies ? " AND " : "") + COL_DELETED + "=" + FALSE) : "")
                    + " " + sortOption
                    + (limit > 0 ? " LIMIT " + limit : "")
                    + ";";
            return db.rawQuery(query, null);
        }
        return null;
    }

    /** Return an array of messages sorted by according current sort order definition and deleted state
     * @param getDeleted whether or not results should include deleted items
     * @param getReplies whether or not results should include items which are replies on other message
     * @param limit Maximum number of items to return or -1 for unlimited
     * @return List of Message items based on database items matching conditions
     */
    public List<MurmurMessage> getMessages(boolean getDeleted, boolean getReplies, int limit){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            Cursor cursor = getMessagesCursor(getDeleted, getReplies, limit);
            if(cursor != null && cursor.getCount() > 0){
                List<MurmurMessage> result = convertToMessages(cursor);
                cursor.close();
                return result;
            }
            cursor.close();
        }
        return new ArrayList<>();
    }

    /** Return a cursor pointing to messages sorted according to current sort order and deleted state
     * @param getDeleted whether or not results should include deleted items
     * @param getReplies whether or not results should include items which are replies on other message
     * @param limit Maximum number of items to return or -1 for unlimited
     * @return Cursor of Message items based on database items matching conditions
     */
    public Cursor getMessagesContainingCursor(String message, boolean getDeleted, boolean getReplies, int limit){
        if(message == null) message = "";
        message = Utils.makeTextSafeForSQL(message);

        SQLiteDatabase db = getWritableDatabase();
        if(db != null) {

            String likeQuery ="";

            String messageNoSpace = message.replaceAll("\\s","");

            if(message.length() == 0 || messageNoSpace.length() == 0) likeQuery =  (COL_MESSAGE + " LIKE '%" + message + "%'");

            if(likeQuery.length() == 0){
                message = message.replaceAll("[\n\"]", " ");

                while(message.charAt(0) == ' '){
                    message = message.substring(1);
                }

                String[] words = message.split("\\s");

                for(int i=0; i<words.length; i++){
                    if(words[i].length() > 0)
                    {
                        if (likeQuery.length() > 0)
                        {
                            likeQuery += " OR ";
                        }
                        likeQuery += " " + COL_MESSAGE + " LIKE '%" + words[i] + "%' ";
                    }
                }
            }

            String query = "SELECT * FROM " + TABLE +
                    " WHERE (" + likeQuery + ")"
                    + (!getReplies ? "AND ("+COL_BIGPARENT+ " IS NULL OR "+COL_BIGPARENT+" NOT IN (SELECT "+COL_MESSAGE_ID+" FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+") AND "+COL_PARENT+" NOT IN (SELECT "+COL_MESSAGE_ID+" FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+ "))" : "")
                    + (!getDeleted ? " AND " + COL_DELETED + "=" + FALSE : "")
                    + " "+sortOption
                    + (limit > 0 ? " LIMIT " + limit : "")
                    + ";";
            return db.rawQuery(query, null);
        }
        return null;
    }

    /** Return a cursor pointing to messages sorted according to current sort order and deleted state
     * @param getDeleted whether or not results should include deleted items
     * @param getReplies whether or not results should include items which are replies on other message
     * @param limit Maximum number of items to return or -1 for unlimited
     * @return Cursor of Message items based on database items matching conditions
     */
    public Cursor getFavoriteMessagesContainingCursor(String message, boolean getDeleted, boolean getReplies, int limit){
        if(message == null) message = "";
        message = Utils.makeTextSafeForSQL(message);

        SQLiteDatabase db = getWritableDatabase();
        if(db != null) {

            String likeQuery ="";

            String messageNoSpace = message.replaceAll("\\s","");

            if(message.length() == 0 || messageNoSpace.length() == 0) likeQuery =  (COL_MESSAGE + " LIKE '%" + message + "%'");

            if(likeQuery.length() == 0){
                message = message.replaceAll("[\n\"]", " ");

                while(message.charAt(0) == ' '){
                    message = message.substring(1);
                }

                String[] words = message.split("\\s");

                for(int i=0; i<words.length; i++){
                    if(words[i].length() > 0) {
                        if (likeQuery.length() > 0) {
                            likeQuery += " OR ";
                        }

                        likeQuery += " " + COL_MESSAGE + " LIKE '%" + words[i] + "%' ";
                    }
                }
            }

            String query = "SELECT * FROM " + TABLE +
                    " WHERE "+COL_FAVIRITE+"="+TRUE+" AND ("+ likeQuery+")"
                    + (!getReplies ? "AND ("+COL_BIGPARENT+ " IS NULL OR "+COL_BIGPARENT+" NOT IN (SELECT "+COL_MESSAGE_ID+" FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+") AND "+COL_PARENT+" NOT IN (SELECT "+COL_MESSAGE_ID+" FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+ "))" : "")
                    + (!getDeleted ? " AND " + COL_DELETED + "=" + FALSE : "")
                    + " "+sortOption
                    + (limit > 0 ? " LIMIT " + limit : "")
                    + ";";
            return db.rawQuery(query, null);
        }
        return null;
    }

    /** Return an array of messages sorted by according their priority and deleted state
     * @param getDeleted whether or not results should include deleted items
     * @param limit Maximum number of items to return or -1 for unlimited
     * @return List of Message items based on database items matching conditions
     */
    public List<MurmurMessage> getMessagesContaining(String message, boolean getDeleted, int limit){
        if(message == null) message = "";

        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            Cursor cursor = getMessagesContainingCursor(message, getDeleted, true, limit);
            if(cursor != null && cursor.getCount() > 0){
                List<MurmurMessage> result = convertToMessages(cursor);
                cursor.close();
                return result;
            }
            cursor.close();
        }
        return new ArrayList<>();
    }

    /** Return a single message matching supplied text or null if no match can be found.
     * @param getDeleted whether or not results should include deleted items
     * @return A Message item based on database item matching conditions or null
     */
    private Cursor getMessageCursor(String message, boolean getDeleted) throws IllegalArgumentException{
        if(message == null || message.isEmpty()) throw new IllegalArgumentException("Message cannot be empty or null ["+message+"].");

        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            String query = "SELECT * FROM " + TABLE + " WHERE " + COL_MESSAGE + "='" + Utils.makeTextSafeForSQL(message) + "'"
                    +(!getDeleted ? " AND "+COL_DELETED+"="+FALSE : "")
                    +" LIMIT 1;";
            return db.rawQuery(query, null);
        }
        return null;
    }

    /** Return a single message matching supplied text or null if no match can be found.
     * @param getDeleted whether or not results should include deleted items
     * @return A Message item based on database item matching conditions or null
     */
    private MurmurMessage getMessage(String message, boolean getDeleted) throws IllegalArgumentException{
        if(message == null || message.isEmpty()) throw new IllegalArgumentException("Message cannot be empty or null ["+message+"].");

        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            Cursor cursor = getMessageCursor(message, getDeleted);
            if(cursor.getCount() > 0){
                MurmurMessage result = convertToMessages(cursor).get(0);
                cursor.close();
                return result;
            }
            cursor.close();
        }
        return null;
    }

    /** return if message exists in database and is not in removed state **/
    public boolean contains(String message){
        return getMessage(message, false) != null;
    }

    /** return if message exists in database, even if is in removed state **/
    public boolean containsOrRemoved(String message){
        return getMessage(message, true) != null;
    }

    /** return the message in position K from the database. K position is calculated
     * after sorting results based on trust. removed messages do not count.
     * @param position position of the message to be returned
     * @return Message in the K position based on priority or null if position too high
     */
    public MurmurMessage getKthMessage(int position){
        List<MurmurMessage> result = getMessages(false, false, position + 1);
        return (result.size() > position) ? result.get(position) : null;
    }

    /**
     * Adds the given message with the given priority.
     *
     * @param message The message to add.
     * @param trust The trust to associate with the message. The trust must
     *                 be [0,1].
     * @param priority The priority to associate with the message.
     * @param pseudonym The senders pseudonym.
     * @param enforceLimit whether or not the trust should be streamlined to the limits
     *                     if set to false and value is outside of limit, an exception is thrown
     * @return Returns true if the message was added. If message already exists, update its values
     */
    public boolean addMessage(Context context, String messageId, String message, double trust, double priority, String pseudonym, long timestamp,boolean enforceLimit, long timebound, Location location, String parent, boolean isRead, int minContactsHop, int hop, String exchange, String bigparent){

        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            if (enforceLimit) {
                trust = streamlineTrust(trust);
            } else {
                checkTrust(trust);
            }

            if(SecurityManager.getCurrentProfile(context).getFeedSize() > 0) {
                Cursor cursor = db.rawQuery("SELECT " + COL_ROWID + " FROM " + TABLE + " WHERE " + COL_DELETED + "=" + FALSE + " ORDER BY " + COL_ROWID + " ASC;", null);
                int overflow = cursor.getCount() - SecurityManager.getCurrentProfile(context).getFeedSize();
                cursor.close();
                if (overflow >= 0) {
                    db.execSQL("UPDATE " + TABLE + " SET " + COL_DELETED + "=" + TRUE + " WHERE " + COL_ROWID +
                            " IN (SELECT " + COL_ROWID + " FROM " + TABLE + " WHERE " + COL_DELETED + "=" + FALSE + " ORDER BY " + COL_ROWID + " ASC LIMIT " + (1 + overflow) + ");");
                }
            }

            //update inserted message in case a better big parent can be found locally
            Cursor cursr = db.rawQuery("SELECT "+COL_BIGPARENT+" FROM "+TABLE+" WHERE "+COL_MESSAGE_ID+"='"+bigparent+"' limit 1;",null);
            if(cursr.getCount() > 0){
                cursr.moveToFirst();
                String tempBigparent = cursr.getString(cursr.getColumnIndex(COL_BIGPARENT));
                if(tempBigparent != null) bigparent = tempBigparent;
            }
            cursr.close();

            // update descendants with this message's big parent
            if(bigparent != null) db.execSQL("UPDATE "+TABLE+" SET "+COL_BIGPARENT+"='"+bigparent+"' WHERE "+COL_PARENT+"='"+messageId+"';");

            if(message.length() > MAX_MESSAGE_SIZE) message = message.substring(0, MAX_MESSAGE_SIZE);

            Calendar tempCal = Calendar.getInstance();
            tempCal.setTimeInMillis(timestamp);
            Calendar reducedTimestamp = Utils.reduceCalendarMin(tempCal);

            if(containsOrRemoved(message)) {
                db.execSQL("UPDATE "+TABLE+" SET "
                        +COL_TRUST+"="+trust+","
                        +COL_DELETED+"="+FALSE+","
                        + COL_LIKES +"="+priority+","
                        +COL_PSEUDONYM+"='"+pseudonym+"',"
                        +COL_BIGPARENT+"='"+bigparent+"',"
                        +COL_PARENT+"='"+parent+"',"
                        + COL_READ +"="+(isRead ? TRUE : FALSE)+","
                        + ((location != null) ? (COL_LATLONG+"='"+location.getLatitude()+" "+location.getLongitude()+"',") : "")
                        +COL_TIMESTAMP+"="+reducedTimestamp.getTimeInMillis()+","
                        + ((exchange != null) ? (COL_EXCHANGE+"="+exchange+",") : "")
                        +COL_EXPIRE+"="+timebound
                        +" WHERE " + COL_MESSAGE + "='" + message + "';");
                log.debug( "Message was already in store and was simply updated.");
            } else {
                ContentValues content = new ContentValues();
                content.put(COL_MESSAGE_ID, messageId);
                content.put(COL_MESSAGE, message);
                content.put(COL_TRUST, trust);
                content.put(COL_LIKES, priority);
                if(location != null) content.put(COL_LATLONG, location.getLatitude()+" "+location.getLongitude());
                content.put(COL_PSEUDONYM, pseudonym);
                content.put(COL_EXPIRE, timebound);
                content.put(COL_TIMESTAMP, reducedTimestamp.getTimeInMillis());
                content.put(COL_BIGPARENT, bigparent);
                content.put(COL_PARENT, parent);
                content.put(COL_READ, isRead ? TRUE : FALSE);
                if(exchange != null) content.put(COL_EXCHANGE, exchange);
                content.put(COL_MIN_CONTACTS_FOR_HOP, minContactsHop);
                content.put(COL_HOP, hop);
                db.insert(TABLE, null, content);
                log.debug( "Message added to store.");
            }
            return true;
        }
        log.debug( "Message not added to store, either message or database is null. ["+message+"]");
        return false;
    }

    /**
     * Remove the given message from the store, the message data is retained with its deleted
     * state set to true.
     *
     * @param message The message to remove.
     * @return Returns true if the message was removed. If the message was not
     * found, returns false.
     */
    public boolean removeMessage(String message){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            db.execSQL("UPDATE " + TABLE + " SET " + COL_DELETED + "=" + TRUE + " WHERE " + COL_MESSAGE + "='" + message + "';");
            return  true;
        }
        log.debug( "Message not added to store, either message or database is null. ["+message+"]");
        return false;
    }

    /**
     * Remove the given all checked messages from the store, the message data is retained with its deleted
     * state set to true.
     *
     * @return Returns true if the message was removed. If the message was not
     * found, returns false.
     */
    public boolean removeCheckedMessage(){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            db.execSQL("UPDATE " + TABLE + " SET " + COL_DELETED + "=" + TRUE + " WHERE " + COL_CHECKED + "=" + TRUE + ";");
            return  true;
        }
        log.debug( "Message not added to store, database is null.");
        return false;
    }

    /**
     * Delete the given message from the store. this will completely remove the data from storage
     *
     * @param message The message to remove.
     * @return Returns true if the message was removed or not found, false otherwise
     */
    public boolean deleteMessage(String message) {
        SQLiteDatabase db = getWritableDatabase();
        if (db != null && message != null) {
            db.execSQL("DELETE FROM " + TABLE + " WHERE " + COL_MESSAGE + "='" + message + "';");
            return true;
        }
        log.debug( "Message not deleted from store, either message or database is null. [" + message + "]");
        return false;
    }

    /** return the amount of items in the database.
     *
     * @param countDeleted if set to true count will include items marked as deleted
     * @return number of items in the database.
     */
    public long getMessageCount(boolean countDeleted, boolean countReplies){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null){
            String query = "";
            if(!countDeleted) {
                query += COL_DELETED + "=" + FALSE;
                if(!countReplies)
                    query += " AND ";
            }
            if(!countReplies)
            {
                query += "(" + COL_BIGPARENT + " IS NULL OR " + COL_BIGPARENT + " NOT IN (SELECT " + COL_MESSAGE_ID + " FROM " + TABLE + " WHERE " + COL_DELETED + "=" + FALSE + ") AND " + COL_PARENT + " NOT IN (SELECT " + COL_MESSAGE_ID + " FROM " + TABLE + " WHERE " + COL_DELETED + "=" + FALSE + "))";
            }
            return DatabaseUtils.queryNumEntries(db, TABLE, query);
        }
        return 0;
    }

    /** return the trust of the given message or 0 if message not exists**/
    public double getTrust(String message){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            Cursor cursor = db.rawQuery("SELECT "+COL_TRUST+" FROM "+TABLE+" WHERE "+COL_MESSAGE+"='"+Utils.makeTextSafeForSQL(message)+"';", null);
            if(cursor.getCount() > 0){
                cursor.moveToFirst();
                return cursor.getDouble(cursor.getColumnIndex(COL_TRUST));
            }
        }
        return 0;
    }

    /** return the priority of the given message or 0 if message not exists**/
    public double getPriority(String message){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            Cursor cursor = db.rawQuery("SELECT "+ COL_LIKES +" FROM "+TABLE+" WHERE "+COL_MESSAGE+"='"+Utils.makeTextSafeForSQL(message)+"';", null);
            if(cursor.getCount() > 0){
                cursor.moveToFirst();
                return cursor.getInt(cursor.getColumnIndex(COL_LIKES));
            }
        }
        return 0;
    }

    /**
     * Update the priority of a message, if it exists in the store
     *
     * @param message      The message whose priority should be changed.
     * @param trust The new trust to set.
     * @param enforceLimit whether or not the new priority should be streamlined to limits
     *                      if set to false and priority is outside of limit an exception is thrown
     * @return True if the message was in the store (and its priority was changed),
     * false otherwise.
     */
    public boolean updateMessage(String message, double trust, boolean enforceLimit/*, int likes*/) {
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            if(enforceLimit){
                trust = streamlineTrust(trust);
            } else {
                checkTrust(trust);
            }
            db.execSQL("UPDATE "+TABLE+" SET "
                            +COL_TRUST+"="+trust
                            //+COL_LIKES+"="+likes+","
                    +" WHERE "+COL_MESSAGE+"='"+Utils.makeTextSafeForSQL(message)+"';");

            log.debug( "Message trust changed in the store.");
            return true;
        }
        log.debug( "Message was not edited, either message or database is null. ["+message+"]");
        return false;
    }

    /**
     * Update the priority of a message, if it exists in the store
     *
     * @param message      The message whose priority should be changed.
     * @param priority The new priority to set.
     * @return True if the message was in the store (and its priority was changed),
     * false otherwise.
     */
    public boolean updateMessage(String message, int priority) {
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            db.execSQL("UPDATE "+TABLE+" SET "+ COL_LIKES +"="+priority+" WHERE "+COL_MESSAGE+"='"+Utils.makeTextSafeForSQL(message)+"';");

            log.debug("Message priority changed in the store.");
            return true;
        }
        log.debug( "Message was not edited, either message or database is null. ["+message+"]");
        return false;
    }

    /**
     * Update the priority of a message, if it exists in the store
     *
     * @param message      The message whose priority should be changed.
     * @param priority The new priority to set.
     * @return True if the message was in the store (and its priority was changed),
     * false otherwise.
     */
    public boolean updatePriority(String message, int priority) {
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            db.execSQL("UPDATE "+TABLE+" SET "+ COL_LIKES +"="+priority+" WHERE "+COL_MESSAGE+"='"+Utils.makeTextSafeForSQL(message)+"';");

            log.debug("Message priority changed in the store.");
            return true;
        }
        log.debug( "Message was not edited, either message or database is null. ["+message+"]");
        return false;
    }

    /** Updating the priority of a message by either +1 or -1 and set the message state as liked or not liked
     *
     * @param message the message to edit
     * @param like if to set the like status of the message to true or false
     * @return true if message was found and edited, false otherwise (false is also returned if message was already in the liked status
     */
    public boolean likeMessage(String message, boolean like){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            int likedStatus = like ? FALSE : TRUE;
            Cursor c = db.rawQuery("SELECT "+COL_LIKES+" FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+" AND "+COL_LIKED+"="+likedStatus+" AND "+COL_MESSAGE+" ='"+Utils.makeTextSafeForSQL(message)+"';",null);
            c.moveToFirst();
            if(c.getCount() > 0){
                int likes = c.getInt(c.getColumnIndex(COL_LIKES)) + (like ? 1 : -1);
                c.close();
                likes = Math.max(0, likes);
                db.execSQL("UPDATE "+TABLE+" SET "+COL_LIKED+"="+(like ? TRUE : FALSE)+","+COL_LIKES +"="+likes+" WHERE "+COL_DELETED+" ="+FALSE+" AND "+COL_LIKED+"="+likedStatus+" AND "+COL_MESSAGE+" ='"+Utils.makeTextSafeForSQL(message)+"';");
                return true;
            }
        }
        log.debug( "Message was not edited, either message or database is null. ["+message+"]");
        return false;
    }

    /** set the favorite state of the specified message as true or false */
    public boolean favoriteMessage(String message, boolean favorite){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            db.execSQL("UPDATE " + TABLE + " SET " + COL_FAVIRITE + "=" + (favorite ? TRUE : FALSE) + " WHERE " + COL_DELETED + " =" + FALSE + " AND " + COL_MESSAGE + " ='" + Utils.makeTextSafeForSQL(message) + "';");
            return true;
        }
        return false;
    }

    /** set the checked state of the specified message as true or false */
    public boolean checkMessage(String message, boolean check){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            db.execSQL("UPDATE " + TABLE + " SET " + COL_CHECKED + "=" + (check ? TRUE : FALSE) + " WHERE " + COL_DELETED + " =" + FALSE + " AND " + COL_MESSAGE + " ='" + Utils.makeTextSafeForSQL(message) + "';");
            return true;
        }
        return false;
    }

    /** set the checked state of the all messages as true or false */
    public boolean checkAllMessages(boolean check, boolean checkReplies){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            String parentOnly = " AND ("+COL_BIGPARENT+ " IS NULL OR "+COL_BIGPARENT+" NOT IN (SELECT "+COL_MESSAGE_ID+" FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+") AND "+COL_PARENT+" NOT IN (SELECT "+COL_MESSAGE_ID+" FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+ "))";
            db.execSQL("UPDATE " + TABLE + " SET " + COL_CHECKED + "=" + (check ? TRUE : FALSE) + " WHERE (" + COL_DELETED + " =" + FALSE + ") " + (checkReplies ? "" : parentOnly) + " ;");
            return true;
        }
        return false;
    }

    /** set the checked state of the all messages as true or false */
    public boolean checkAllQueriedMessages(boolean check, String query){
        if(query == null || query.length() == 0){
            return checkAllMessages(check, false);
        }

        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            String parentOnly = " AND ("+COL_BIGPARENT+ " IS NULL OR "+COL_BIGPARENT+" NOT IN (SELECT "+COL_MESSAGE_ID+" FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+") AND "+COL_PARENT+" NOT IN (SELECT "+COL_MESSAGE_ID+" FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+ "))";
            db.execSQL("UPDATE " + TABLE + " SET " + COL_CHECKED + "=" + (check ? TRUE : FALSE) + " WHERE " + COL_DELETED + " =" + FALSE + " " + query + parentOnly + ";");
            return true;
        }
        return false;
    }

    /** set the checked state of the all messages as true or false */
    public boolean checkAllMessagesContaining(boolean check, String message){
        if(message == null || message.length() == 0){
            return checkAllMessages(check, false);
        }
        message = Utils.makeTextSafeForSQL(message);

        SQLiteDatabase db = getWritableDatabase();
        if(db != null){

            String likeQuery ="";
            String messageNoSpace = message.replaceAll("\\s","");
            if(message.length() == 0 || messageNoSpace.length() == 0) likeQuery =  (COL_MESSAGE + " LIKE '%" + message + "%'");
            if(likeQuery.length() == 0){
                message = message.replaceAll("[\n\"]", " ");
                while(message.charAt(0) == ' '){
                    message = message.substring(1);
                }
                String[] words = message.split("\\s");
                for(int i=0; i<words.length; i++){
                    if(i > 0){
                        likeQuery +=" OR ";
                    }
                    likeQuery += " "+COL_MESSAGE + " LIKE '%"+words[i]+"%' ";
                }
            }
            String parentOnly = " AND ("+COL_BIGPARENT+ " IS NULL OR "+COL_BIGPARENT+" NOT IN (SELECT "+COL_MESSAGE_ID+" FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+") AND "+COL_PARENT+" NOT IN (SELECT "+COL_MESSAGE_ID+" FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+ "))";
            db.execSQL("UPDATE " + TABLE + " SET " + COL_CHECKED + "=" + (check ? TRUE : FALSE) + " WHERE " + COL_DELETED + " =" + FALSE+" AND ("+likeQuery+")" + parentOnly + ";");
            return true;
        }
        return false;
    }

    /** return a cursor pointing at all the messages in the store with checked state set to true */
    public Cursor getCheckedMessages(){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            return db.rawQuery("SELECT * FROM " + TABLE + " WHERE " + COL_CHECKED + "="+TRUE + " AND " +COL_DELETED+"="+FALSE+ ";", null);
        }
        return null;
    }

    /** Return the current version of the store */
    public String getStoreVersion(){
        if(storeVersion == null) updateStoreVersion();

        return  storeVersion;
    }

    /** Randomize a version code for the store and set it*/
    public void updateStoreVersion(){
        storeVersion = UUID.randomUUID().toString();
    }

    /** set the read state of the supplied message to either read or unread */
    public boolean setRead(String message, boolean isRead){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null && message != null){
            int read = isRead ? TRUE : FALSE;
            db.execSQL("UPDATE " + TABLE + " SET " + COL_READ + "=" + read + " WHERE " + COL_MESSAGE + "='" + Utils.makeTextSafeForSQL(message) + "';");

            log.debug("Message read state changed in the store.");
            return true;
        }
        log.debug( "Message was not edited, either message or database is null. ["+message+"]");
        return false;
    }

    /** reset the local storage to make all the messages marked as read */
    public boolean setAllAsRead(){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            db.execSQL("UPDATE "+TABLE+" SET "+COL_READ+"="+TRUE+";");

            log.debug("Messages read state changed in the store.");
            //clear exchange history
            ExchangeHistoryTracker.getInstance().resetExchangeCount();
            return true;
        }
        log.debug( "Messages not edited, database is null.");
        return false;
    }

    public long getUnreadCount(){
        SQLiteDatabase db = getWritableDatabase();
        if(db != null){
            return DatabaseUtils.queryNumEntries(db, TABLE, COL_READ + "=" + FALSE);
        }
        return  0;
    }

    /** completely delete records from the database based on passed security profile
     *
     * @param currentProfile
     */
    public void deleteOutdatedOrIrrelevant(SecurityProfile currentProfile){

        if(currentProfile == null || !currentProfile.isAutodelete()) return;

        SQLiteDatabase db = getWritableDatabase();
        if(db == null) return;

        Calendar reducedAge = Utils.reduceCalendar(Calendar.getInstance());

        long ageThreshold = reducedAge.getTimeInMillis() - TimeUnit.DAYS.toMillis(currentProfile.getAutodeleteAge());

        db.execSQL("DELETE FROM "+TABLE+" WHERE "/*"UPDATE " + TABLE + " SET " + COL_DELETED + "=" + TRUE + " WHERE "*/
                + COL_TRUST+"<="+(currentProfile.isAutodelete() ? currentProfile.getAutodeleteTrust() : 0) //delete untrusted
                + " OR ("+COL_TIMESTAMP+"> 0 AND "+COL_TIMESTAMP+"<"+ageThreshold+")" //delete old
                + " OR ("+COL_EXPIRE+"> 0 AND "+COL_TIMESTAMP+">0 AND ("+COL_EXPIRE  +"+"+COL_TIMESTAMP+") <"+System.currentTimeMillis() //delete expired (self-destruct)
                +");"
        );
    }

    public Cursor getMessagesByQuery(String query){
        SQLiteDatabase db = getWritableDatabase();
        if(db == null || query == null) return null;

        String pretext = "SELECT * FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+" ";
        String posttext = " "+sortOption;

        try {
            Cursor cursor = db.rawQuery(pretext + query + posttext, null);
            return cursor;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public void deleteByLikes(int likes){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            db.execSQL("UPDATE " + TABLE + " SET " + COL_DELETED + "=" + TRUE + " WHERE " + COL_LIKES + "<=" + likes + ";");
        }
    }

    public void deleteTree(String bigparent){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null && bigparent != null) {
            db.execSQL("UPDATE " + TABLE + " SET " + COL_DELETED + "=" + TRUE + " WHERE "+COL_MESSAGE_ID+" = '"+bigparent+"' OR "+ COL_BIGPARENT + "='" + bigparent + "';");
        }
    }

    public int getMessagesByLikeCount(int likes){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+" AND "+COL_LIKES + "<=" + likes + ";" ,null);
            int count = cursor.getCount();
            cursor.close();
            return count;
        }
        return 0;
    }

    public void deleteByTrust(float trust){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            db.execSQL("UPDATE " + TABLE + " SET " + COL_DELETED + "=" + TRUE + " WHERE " + COL_TRUST + "<=" + trust + ";");
        }
    }

    public int getMessagesByTrustCount(float trust){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+" AND "+COL_TRUST + "<=" + trust + ";" ,null);
            int count = cursor.getCount();
            cursor.close();
            return count;
        }
        return 0;
    }

    public void deleteBySender(String sender){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null && sender != null && sender.length() > 0) {
            db.execSQL("UPDATE " + TABLE + " SET " + COL_DELETED + "=" + TRUE + " WHERE " + COL_PSEUDONYM + "='" + sender + "';");
        }
    }

    public int getMessagesBySenderCount(String sender){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null && sender != null && sender.length() > 0) {
            Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+" AND "+COL_PSEUDONYM + "='" + sender + "';" ,null);
            int count = cursor.getCount();
            cursor.close();
            return count;
        }
        return 0;
    }

    public void deleteByExchange(String exchange){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null && exchange != null && exchange.length() > 0) {
            db.execSQL("UPDATE " + TABLE + " SET " + COL_DELETED + "=" + TRUE + " WHERE " + COL_EXCHANGE + "='" + exchange + "';");
        }
    }

    public int getMessagesByExchangeCount(String exchange){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+" AND "+COL_EXCHANGE + "='" + exchange + "';" ,null);
            int count = cursor.getCount();
            cursor.close();
            return count;
        }
        return 0;
    }

    public void purgeStore(){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            onCreate(db);
        }
    }

    public void setSortOption(String[] columns, boolean ascending){

        boolean needSecondaryByRowId = true;
        for(String col : columns){
            if(col.equals(COL_ROWID)) needSecondaryByRowId = false;
        }

        String options = "";
        for (int i = 0; i < defaultSort.length; i++) {
            options += defaultSort[i];
            if (i < defaultSort.length - 1 ||columns != null) {
                options += ",";
            }
        }

        if(columns != null) {
            for (int i = 0; i < columns.length; i++) {
                options += columns[i];
                if (i < columns.length - 1) {
                    options += ",";
                }
            }
        }
        sortOption = "ORDER BY "+options+(ascending ? " ASC" : " DESC");


        if(needSecondaryByRowId){
            sortOption += ", "+COL_ROWID+" DESC";
        }
    }

    /** return comments of a certain message parent */
    public Cursor getComments(String parentId){
        SQLiteDatabase db = getReadableDatabase();
        if(db != null){
            return db.rawQuery("SELECT * FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+" AND ("+COL_PARENT+"='"+parentId+"' OR "+COL_BIGPARENT+"='"+parentId+"') "+sortOption+";",null);
        }
        return null;
    }

    /** return comments of a certain message parent */
    public Cursor getCommentsByQuery(String parentId, String query){
        return getMessagesByQuery("AND ("+COL_PARENT+"='"+parentId+"' OR "+COL_BIGPARENT+"='"+parentId+"') "+query);
    }

    /** return comments of a certain message parent containing the query in the message */
    public Cursor getCommentsContaining(String parentId, String query){
        if(query == null) query = "";
        query = Utils.makeTextSafeForSQL(query);
        String likeQuery = "";
        String queryNoSpace = query.replaceAll("\\s","");

        if(query.length() == 0 || queryNoSpace.length() == 0) likeQuery = (COL_MESSAGE + " LIKE '%" + query + "%'");

        if(likeQuery.length() == 0){
            query = query.replaceAll("[\n\"]", " ");

            while(query.charAt(0) == ' '){
                query = query.substring(1);
            }

            String[] words = query.split("\\s");

            for(int i=0; i<words.length; i++){
                if(words[i].length() > 0)
                {
                    if (likeQuery.length() > 0)
                    {
                        likeQuery += " OR ";
                    }
                    likeQuery += " " + COL_MESSAGE + " LIKE '%" + words[i] + "%' ";
                }
            }
        }


        return getCommentsByQuery(parentId, "AND (" + likeQuery + ")");
    }

    public int getCommentCount(String parentId){
        Cursor c = getComments(parentId);
        int count = c.getCount();
        c.close();
        return count;
    }

    /** return a cursor with a single message based on passed messageId */
    public Cursor getMessageById(String messageId){
        SQLiteDatabase db = getReadableDatabase();
        if(db != null){
            return db.rawQuery("SELECT * FROM "+TABLE+" WHERE "+COL_DELETED+"="+FALSE+" AND "+COL_MESSAGE_ID+"='"+messageId+"' LIMIT 1;",null);
        }
        return null;
    }

    public List<MurmurMessage> getMessagesForExchange(int sharedContacts){
        SQLiteDatabase db = getReadableDatabase();
        if(db != null){
            return convertToMessages(db.rawQuery("SELECT * FROM " + TABLE + " WHERE "
                    + COL_DELETED + "=" + FALSE +
                    " AND ((" + COL_HOP + " = " + 0 + " AND " + COL_MIN_CONTACTS_FOR_HOP + " > 0 AND " + COL_MIN_CONTACTS_FOR_HOP + " <= " + sharedContacts +
                        ") OR (" + COL_MIN_CONTACTS_FOR_HOP + " <= 0))" +
                    " ORDER BY "+COL_ROWID+" DESC;"
                    , null));
        }
        return new ArrayList<MurmurMessage>();
    }
}