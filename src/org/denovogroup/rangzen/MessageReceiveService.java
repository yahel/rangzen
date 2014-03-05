package org.denovogroup.rangzen;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MessageReceiveService extends Service {

	private static final String TAG = "MessageReceiveService";
	private DatagramSocket socket;
	public static final String localInterfaceName = "10.0.2.15";
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		Log.i(TAG, "starting message receive loop");
		Thread t = new Thread(new Runnable() {
			public void run() {
				receiveMessageLoop();
			}
		});
		t.start();

		// Ask Android to shut the service down if the app is killed.
		// This is probably not the correct behavior but handling the
		// app shutting down presumably requires some logic queueing up 
		// and saving the messages we receive, which will be implemented
		// later on.
		return START_NOT_STICKY;
	}
	
	private void receiveMessageLoop() {
		Log.i(TAG, "started message receive loop");
		// receive messages, etc.
		try {
			InetAddress localInterfaceAddress = 
					InetAddress.getByName(localInterfaceName);
			socket = new DatagramSocket(
							MessageSendIntentService.DESTINATION_PORT,
							localInterfaceAddress);
		} catch (SocketException e) {
			// :(
		} catch (UnknownHostException e) {
			// :(
		}
		while (true) {
			byte[] receiveBuffer = new byte[1500];
			DatagramPacket receivedPacket =
					new DatagramPacket(receiveBuffer, receiveBuffer.length);
			try {
				socket.receive(receivedPacket);
				byte[] bytes = receivedPacket.getData();
				String packetJSONString = 
						new String(bytes, 0, receivedPacket.getLength());
				JSONObject packetJSONObject = new JSONObject(packetJSONString);
				String message = packetJSONObject.getString("message");
				Log.i(TAG, "received a message");
				Log.i(TAG, "received message: "+message);
			} catch (IOException e) {
				// :(
			} catch (JSONException e) {
				// Wasn't actually JSON...
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// This service is not meant to be used through binding
		return null;
	}

}
