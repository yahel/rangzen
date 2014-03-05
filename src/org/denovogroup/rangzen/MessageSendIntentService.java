package org.denovogroup.rangzen;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class MessageSendIntentService extends IntentService {
	public static final String MESSAGE_TO_SEND = "MESSAGE_TO_SEND";
	public static final int DESTINATION_PORT = 51689;
	public static final String DESTINATION_ADDRESS = "10.0.2.2";
	
	public static final String TAG = "MessageSendIntentService";

	// Constructor
	public MessageSendIntentService() {
		super("MessageSendIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		//send message
		String message = intent.getStringExtra(MESSAGE_TO_SEND);
		
		Map<String, String> keyValuePairs = new HashMap<String, String>();
		keyValuePairs.put("message", message);
		JSONObject messageJSONObject = new JSONObject(keyValuePairs);
		
		String messageJSON = messageJSONObject.toString();
		
		Log.i("MessageSendIntentService", "sending message "+message);
		
		try {
			DatagramSocket socket = new DatagramSocket();
			InetAddress destinationAddress = 
					InetAddress.getByName(DESTINATION_ADDRESS);
			int messageLength = messageJSON.length();
			byte[] messageByteArray = messageJSON.getBytes();
			DatagramPacket packet = new DatagramPacket(messageByteArray, 
													   messageLength,
													   destinationAddress,
													   DESTINATION_PORT);
			socket.send(packet);
		} catch (SocketException e) {
			Log.e(TAG, "Socket Exception");
			// Failure, retry or give up?
		} catch (UnknownHostException e) {
			Log.e(TAG, "Unknown Host Exception");
			// DNS failed, shouldn't happen with raw IP
		} catch (IOException e) {
			Log.e(TAG, "IOException");
			// Sending packet failed
		}

	}

}
