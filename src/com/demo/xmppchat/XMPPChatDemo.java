package com.demo.xmppchat;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
//import org.jivesoftware.smackx.packet.StreamInitiation.File;


import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import com.unity3d.player.UnityPlayer;
import com.demo.xmppchat.Constants;

public class XMPPChatDemo {

	public static final String HOST = "sandbox-frienger.jinsei-iroiro.com";
	public static final int PORT = 5222;
	public static final String SERVICE = "sandbox-frienger.jinsei-iroiro.com";
	public String USERNAME = "thangdepzai";
	public String PASSWORD = "123456";
	
	public static final String XMPPObjet = "XMPPObject";
	public static final String XMPPMethod = "recieveMessage";

	private XMPPConnection connection;
	private ArrayList<String> messages = new ArrayList<String>();
	//private Handler mHandler = new Handler();
	
	private static XMPPChatDemo _instance; 

	
	// singleton
	public static XMPPChatDemo getInstance()
	{
		if(_instance == null)
			_instance = new XMPPChatDemo();
		return _instance;
	}
	
	public void init(String username, String password){
		this.USERNAME = username;
		this.PASSWORD = password;
	}
	
	
	public XMPPChatDemo()
	{
		
	}
	
	public void sendMessage(String to,String content)
	{
		System.out.println("start send message");
		Constants.receipient = to;
		Message msg = new Message(to, Message.Type.chat);
		msg.setBody(content);				
		if (connection != null) {
			connection.sendPacket(msg);
			messages.add(connection.getUser() + ":");
			messages.add(content);
			// TODO
//			UnityPlayer.UnitySendMessage(arg0, arg1, arg2)
			if (content.length() >= 8) {
				Log.e("IMG URL 1 :",content.substring(0, 7));
				if (content.substring(0, 7).equals("@image:")) {
					String imgUrl = content.substring(7,content.length() - 1);
					Log.e("IMG URL 2 :", imgUrl);
					//saveImages(imgUrl);
					content = imgUrl;
				}
			}
			String message = "me" + "thang,khoa,ngoc,huy"  + content;
			System.out.println("content : " + content );
			UnityPlayer.UnitySendMessage("XMPPObject", "recieveMessage", message);
			
			System.out.println(" send end "  );
//			setListAdapter();
		}
	}

	/**
	 * Called by Settings dialog when a connection is establised with the XMPP
	 * server
	 * 
	 * @param connection
	 */
	public void setConnection(XMPPConnection connection) {
		this.connection = connection;
		Constants.connection = connection;
		if (connection != null) {
			// Add a packet listener to get messages sent to us
			PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
			connection.addPacketListener(new PacketListener() {
				@Override
				public void processPacket(Packet packet) {
					Message message = (Message) packet;
					if (message.getBody() != null) {
						String fromName = StringUtils.parseBareAddress(message.getFrom());
						Log.i("XMPPChatDemoActivity", "Text Recieved " + message.getBody()+ " from " + fromName );
						messages.add(fromName + ":");
						messages.add(message.getBody());
						String msgBody = message.getBody();
						if(msgBody.length() >= 8) {
							if(msgBody.substring(0,7).equals("@image:")){
								String imgUrl = msgBody.substring(7,message.getBody().length() - 1);
								//saveImages(imgUrl);
								msgBody = imgUrl;
							}
						}
						String content = fromName + "thang,khoa,ngoc,huy"  + msgBody;
						UnityPlayer.UnitySendMessage(XMPPObjet, XMPPMethod, content);
//						// Add the incoming message to the list view
//						mHandler.post(new Runnable() {
//							public void run() {
//								setListAdapter();
//							}
//						});
					}
				}
			}, filter);
		}
	}
	public void pickupImage(String toUser){
		Activity root = UnityPlayer.currentActivity;
		Constants.receipient = toUser;
		Intent i = new Intent(root, PickupImageActivity.class);
		root.startActivity(i);
	}


	public void connect() {

//		final ProgressDialog dialog = ProgressDialog.show(this,
//				"Connecting...", "Please wait...", false);
		System.out.println("======================  start connect =====");
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				// Create a connection
				ConnectionConfiguration connConfig = new ConnectionConfiguration(
						HOST, PORT, SERVICE);
				if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
					connConfig.setTruststoreType("AndroidCAStore");
					connConfig.setTruststorePassword(null);
					connConfig.setTruststorePath(null);
				} else {
					connConfig.setTruststoreType("BKS");
				    String path = System.getProperty("javax.net.ssl.trustStore");
				    if (path == null)
				        path = System.getProperty("java.home") + File.separator + "etc"
				            + File.separator + "security" + File.separator
				            + "cacerts.bks";
				    connConfig.setTruststorePath(path);
				}
				
				XMPPConnection connection = new XMPPConnection(connConfig);

				try {
					connection.connect();
					Log.i("XMPPChatDemoActivity",
							"Connected to " + connection.getHost());
				} catch (XMPPException ex) {
					Log.e("XMPPChatDemoActivity", "Failed to connect to "
							+ connection.getHost());
					Log.e("XMPPChatDemoActivity", ex.toString());
					setConnection(null);
				}
				try {
					// SASLAuthentication.supportSASLMechanism("PLAIN", 0);
					connection.login(USERNAME, PASSWORD);
					Log.i("XMPPChatDemoActivity",
							"Logged in as " + connection.getUser());

					// Set the status to available
					Presence presence = new Presence(Presence.Type.available);
					connection.sendPacket(presence);
					setConnection(connection);

					Roster roster = connection.getRoster();
					Collection<RosterEntry> entries = roster.getEntries();
					for (RosterEntry entry : entries) {
						Log.d("XMPPChatDemoActivity",
								"--------------------------------------");
						Log.d("XMPPChatDemoActivity", "RosterEntry " + entry);
						Log.d("XMPPChatDemoActivity",
								"User: " + entry.getUser());
						Log.d("XMPPChatDemoActivity",
								"Name: " + entry.getName());
						Log.d("XMPPChatDemoActivity",
								"Status: " + entry.getStatus());
						Log.d("XMPPChatDemoActivity",
								"Type: " + entry.getType());
						Presence entryPresence = roster.getPresence(entry
								.getUser());

						Log.d("XMPPChatDemoActivity", "Presence Status: "
								+ entryPresence.getStatus());
						Log.d("XMPPChatDemoActivity", "Presence Type: "
								+ entryPresence.getType());
						Presence.Type type = entryPresence.getType();
						if (type == Presence.Type.available)
							Log.d("XMPPChatDemoActivity", "Presence AVIALABLE");
						Log.d("XMPPChatDemoActivity", "Presence : "
								+ entryPresence);

					}
				} catch (XMPPException ex) {
					Log.e("XMPPChatDemoActivity", "Failed to log in as "
							+ USERNAME);
					Log.e("XMPPChatDemoActivity", ex.toString());
					setConnection(null);
				}

//				dialog.dismiss();
			}
		});
		t.start();
		System.out.println("");
//		dialog.show();
	}
	
	public String saveImages(String inputUrl){
		String filepath = "";
		try{   
		  URL url = new URL(inputUrl);
		  HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		  urlConnection.setRequestMethod("GET");
		  urlConnection.setDoOutput(false);                   
		  urlConnection.connect();                  
		  File SDCardRoot = Environment.getExternalStorageDirectory().getAbsoluteFile();
		  String filename="Ishine"+System.currentTimeMillis()+".JPEG";   
		  Log.i("Local filename:",""+filename);
		  File dir = new File(SDCardRoot+"/pchat/");
		  if(!dir.exists()){
			  dir.mkdirs();
		  }
          File file = new File(dir, filename);
		  FileOutputStream fileOutput = new FileOutputStream(file);
		  InputStream inputStream = urlConnection.getInputStream();
		  int totalSize = urlConnection.getContentLength();
		  int downloadedSize = 0;   
		  byte[] buffer = new byte[1024];
		  int bufferLength = 0;
		  while ( (bufferLength = inputStream.read(buffer)) > 0 ) 
		  {                 
		    fileOutput.write(buffer, 0, bufferLength);                  
		    downloadedSize += bufferLength;                 
		    Log.i("Progress:","downloadedSize:"+downloadedSize+"totalSize:"+ totalSize) ;
		  }             
		  fileOutput.close();
		  if(downloadedSize==totalSize) 
			  filepath=file.getPath();    
		} 
		catch (MalformedURLException e) 
		{
		  e.printStackTrace();
		} 
		catch (IOException e)
		{
		  filepath=null;
		  e.printStackTrace();
		}
		Log.i("filepath:"," "+filepath) ;
		return filepath;

	}
	

}