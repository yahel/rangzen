package org.denovogroup.rangzen.backend;

import com.parse.ParseInstallation;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Liran on 9/1/2015.
 */
public class EventReportsMaker {

    private static final String USERID_KEY = "Userid";
    private static final String TIME_KEY = "Time";
    private static final String EVENT_TAG_KEY = "Tag";
    private static final String EVENT_ACTION_KEY = "Action";
    private static final String EVENT_MESSAGE_KEY = "Message";
    public static class LogEvent{
        public enum event_tag{
            SOCIAL_GRAPH, MESSAGE, NETWORK, UI
        }

        public static class event_action{
            public enum Message{
                EXCHANGE, REWEETED, POSTED, USER_PRIORITY, DELETED, SYSTEM_PRIORITY
            }

            public enum Network{
                WIFI_STATE, ERROR, FOUND_DEVICE, CONNECTED_DEVICE
            }

            public enum Ui {
                SEARCH_COUNT, HASHTAG_COUNT, FRIENDS_ADDED, NOTIFICATION_COUNT, TYPES_SENT
            }
        }
    }

    private static final String EVENT_PRIORITY_KEY = "Priority";
    private static final String EVENT_SENDER_KEY = "Sender";
    private static final String EVENT_RECEIVER_KEY = "Receiver";
    private static final String EVENT_MESSAGE_ID_KEY = "message_id";
    private static final String EVENT_MUTUAL_FRIENDS_KEY = "mutual_friends";

    public JSONObject reportReceivedFriends(long timestamp){
        try {
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, ParseInstallation.getCurrentInstallation());
            testObject.put(TIME_KEY, timestamp);
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.SOCIAL_GRAPH);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject reportMessageExchange(long timestamp, String sender, String receiver, String messageId, float priority, float mutualFriends){
        try{
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, ParseInstallation.getCurrentInstallation());
            testObject.put(TIME_KEY, timestamp);
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.MESSAGE);
            testObject.put(EVENT_ACTION_KEY, LogEvent.event_action.Message.EXCHANGE);
            testObject.put(EVENT_PRIORITY_KEY, priority);
            testObject.put(EVENT_SENDER_KEY, sender);
            testObject.put(EVENT_RECEIVER_KEY, receiver);
            testObject.put(EVENT_MESSAGE_ID_KEY, messageId);
            testObject.put(EVENT_MUTUAL_FRIENDS_KEY, mutualFriends);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject reportMessageReweeted(long timestamp, String messageId, float priority, String message){
        try{
            JSONObject testObject = new JSONObject();
            testObject.put(USERID_KEY, ParseInstallation.getCurrentInstallation());
            testObject.put(TIME_KEY, timestamp);
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.MESSAGE);
            testObject.put(EVENT_ACTION_KEY, LogEvent.event_action.Message.REWEETED);
            testObject.put(EVENT_PRIORITY_KEY, priority);
            testObject.put(EVENT_MESSAGE_ID_KEY, messageId);
            testObject.put(EVENT_MESSAGE_KEY, message);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject reportMessagePosted(long timestamp, String messageId, float priority, String message){
        try {
            JSONObject testObject = new JSONObject();
            testObject.put(EVENT_TAG_KEY, LogEvent.event_tag.MESSAGE);
            testObject.put(EVENT_ACTION_KEY, LogEvent.event_action.Message.POSTED);
            testObject.put(EVENT_PRIORITY_KEY, priority);
            testObject.put(EVENT_MESSAGE_ID_KEY, messageId);
            testObject.put(EVENT_MESSAGE_KEY, message);
            return testObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
