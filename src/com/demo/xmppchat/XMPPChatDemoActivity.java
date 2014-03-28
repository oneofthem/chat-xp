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
import java.util.Date;

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

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

public class XMPPChatDemoActivity extends Activity {

	String strVideo = "pchatupload";
	private AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(Constants.ACCESS_KEY_ID,Constants.SECRET_KEY));


	public static final String HOST = "sandbox-frienger.jinsei-iroiro.com";
	public static final int PORT = 5222;
	public static final String SERVICE = "sandbox-frienger.jinsei-iroiro.com";
	public static final String USERNAME = "test";
	public static final String PASSWORD = "123456";
	
	/*
	public static final String HOST = "talk.google.com";
	public static final int PORT = 5222;
	public static final String SERVICE = "gmail.com";
	public static final String USERNAME = "cuongoihuhu@gmail.com";
	public static final String PASSWORD = "";
	*/



	private XMPPConnection connection;
	private ArrayList<String> messages = new ArrayList<String>();
	private Handler mHandler = new Handler();

	private EditText recipient;
	private EditText textMessage;
	private ListView listview;

	ImageView imgUploadImage;
	private static final int PHOTO_SELECTED = 1;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		recipient = (EditText) this.findViewById(R.id.toET);

		recipient.setText("thangdepzai@sandbox-frienger.jinsei-iroiro.com");
		textMessage = (EditText) this.findViewById(R.id.chatET);
		listview = (ListView) this.findViewById(R.id.listMessages);
		imgUploadImage = (ImageView)findViewById(R.id.imgUploadImage);
		setListAdapter();
		
		connect();
		
		imgUploadImage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
//				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//				intent.setType("image/*");
//				startActivityForResult(intent, PHOTO_SELECTED);
				Intent intent = new Intent(XMPPChatDemoActivity.this, PickupImageActivity.class);
				startActivity(intent);
			}
		});
		
		// Set a listener to send a chat text message
		Button send = (Button) this.findViewById(R.id.sendBtn);
		send.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				String to = recipient.getText().toString();
				String text = textMessage.getText().toString();

				Log.i("XMPPChatDemoActivity", "Sending text " + text + " to " + to);
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);				
				if (connection != null) {
					connection.sendPacket(msg);
					messages.add(connection.getUser() + ":");
					messages.add(text);
					setListAdapter();
				}
			}
		});
		
	}

	/**
	 * Called by Settings dialog when a connection is establised with the XMPP
	 * server
	 * 
	 * @param connection
	 */
	public void setConnection(XMPPConnection connection) {
		this.connection = connection;
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
						if (message.getBody().length() >= 8) {
							Log.e("IMG URL 1 :",
									message.getBody().substring(0, 7));
							if (message.getBody().substring(0, 7)
									.equals("@image:")) {
								String imgUrl = message.getBody().substring(7,
										message.getBody().length() - 1);
								Log.e("IMG URL 2 :", imgUrl);
								saveImages(imgUrl);
							}
						}
						// Add the incoming message to the list view
						mHandler.post(new Runnable() {
							public void run() {
								setListAdapter();
							}
						});
					}
				}
			}, filter);
		}
	}

	private void setListAdapter() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.listitem, messages);
		listview.setAdapter(adapter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			if (connection != null)
				connection.disconnect();
		} catch (Exception e) {

		}
	}

	public void connect() {

		final ProgressDialog dialog = ProgressDialog.show(this,"Connecting...", "Please wait...", false);

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				// Create a connection
				ConnectionConfiguration connConfig = new ConnectionConfiguration(HOST, PORT, SERVICE);
				XMPPConnection connection = new XMPPConnection(connConfig);

				try {
					connection.connect();
					Log.i("XMPPChatDemoActivity","Connected to " + connection.getHost());
				} catch (XMPPException ex) {
					Log.e("XMPPChatDemoActivity", "Failed to connect to "+ connection.getHost());
					Log.e("XMPPChatDemoActivity", ex.toString());
					setConnection(null);
				}
				try {
					// SASLAuthentication.supportSASLMechanism("PLAIN", 0);
					connection.login(USERNAME, PASSWORD);
					//connection.loginAnonymously();
					Log.i("XMPPChatDemoActivity","Logged in as " + connection.getUser());

					// Set the status to available
					//Presence presence = new Presence(Presence.Type.available);
					setConnection(connection);
					Constants.connection = connection;
					
					Roster roster = connection.getRoster();
					Collection<RosterEntry> entries = roster.getEntries();
					for (RosterEntry entry : entries) {
						Log.d("XMPPChatDemoActivity",
								"--------------------------------------");
						Log.d("XMPPChatDemoActivity", "RosterEntry " + entry);
						Log.d("XMPPChatDemoActivity","User: " + entry.getUser());
						Log.d("XMPPChatDemoActivity","Name: " + entry.getName());
						Log.d("XMPPChatDemoActivity","Status: " + entry.getStatus());
						Log.d("XMPPChatDemoActivity","Type: " + entry.getType());
						Presence entryPresence = roster.getPresence(entry.getUser());

						Log.d("XMPPChatDemoActivity", "Presence Status: "+ entryPresence.getStatus());
						Log.d("XMPPChatDemoActivity", "Presence Type: "+ entryPresence.getType());
						Presence.Type type = entryPresence.getType();
						if (type == Presence.Type.available)
							Log.d("XMPPChatDemoActivity", "Presence AVIALABLE");
						Log.d("XMPPChatDemoActivity", "Presence : "+ entryPresence);
					}
					
				} catch (Exception ex) {
					ex.printStackTrace();
					Log.e("XMPPChatDemoActivity", "Failed to log in as "+ USERNAME);
					Log.e("XMPPChatDemoActivity", ex.toString());
					setConnection(null);
				}

				dialog.dismiss();
			}
		});
		t.start();
		dialog.show();
	}
	
	
	///////////////////////////////////////////////////////////////
	/////////// XU LY UPLOAD FILE LEN SERVER AMAZONE S3 ///////////
	//////////////////////////////////////////////////////////////
	protected void displayAlert(String title, String message) {

		AlertDialog.Builder confirm = new AlertDialog.Builder(this);
		confirm.setTitle(title);
		confirm.setMessage(message);

		confirm.setNegativeButton(
				XMPPChatDemoActivity.this.getString(R.string.ok),
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {

						dialog.dismiss();
					}
				});

		confirm.show().show();
	}

	protected void displayErrorAlert(String title, String message) {

		AlertDialog.Builder confirm = new AlertDialog.Builder(this);
		confirm.setTitle(title);
		confirm.setMessage(message);

		confirm.setNegativeButton(
				XMPPChatDemoActivity.this.getString(R.string.ok),
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {

						XMPPChatDemoActivity.this.finish();
					}
				});

		confirm.show().show();
	}

	String imageName = "";
	private class S3PutObjectTask extends AsyncTask<Uri, Void, S3TaskResult> {

		ProgressDialog dialog;
		long startTime;
		protected void onPreExecute() {
			dialog = new ProgressDialog(XMPPChatDemoActivity.this);
			dialog.setMessage(XMPPChatDemoActivity.this
					.getString(R.string.uploading));
			dialog.setCancelable(false);
			dialog.show();
		}

		protected S3TaskResult doInBackground(Uri... uris) {
			if (uris == null || uris.length != 1) {
				return null;
			}
			// The file location of the image selected.
			Uri selectedImage = uris[0];

			String[] filePathColumn = { MediaStore.Images.Media.DATA };

			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			String filePath = cursor.getString(columnIndex);
			cursor.close();

			S3TaskResult result = new S3TaskResult();
			Log.e("FILE PATH : ", filePath);
		    startTime = System.currentTimeMillis();
		    imageName = Constants.PICTURE_NAME+ System.currentTimeMillis();
			// Put the image data into S3.
			//filePath = "/sdcard/Video/sample1.mp4";
			//Log.e("File Path",filePath);
			try {

				//s3Client.createBucket(Constants.getPictureBucket(strVideo));
				// Content type is determined by file extension.
				
				PutObjectRequest por = new PutObjectRequest(
						Constants.getPictureBucket(strVideo), imageName,
						new java.io.File(filePath));
				s3Client.putObject(por);
			} catch (Exception exception) {

				result.setErrorMessage(exception.getMessage());
			}

			return result;
		}

		protected void onPostExecute(S3TaskResult result) {

			dialog.dismiss();
			long time = System.currentTimeMillis() - startTime;
			Log.e("TIME UPLOAD",time+"");
			if (result.getErrorMessage() != null) {
				displayErrorAlert(
						XMPPChatDemoActivity.this
								.getString(R.string.upload_failure_title),
						result.getErrorMessage());
			}else{
				//new S3GeneratePresignedUrlTask().execute();
				new S3GenerateImagePresignedUrlTask().execute();
			}
		}
	}

	private class S3GeneratePresignedUrlTask extends AsyncTask<Void, Void, S3TaskResult> {
		
		protected S3TaskResult doInBackground(Void... voids) {

			S3TaskResult result = new S3TaskResult();

			try {
				// Ensure that the image will be treated as such.
				ResponseHeaderOverrides override = new ResponseHeaderOverrides();
				override.setContentType("video/mp4");

				// Generate the presigned URL.

				// Added an hour's worth of milliseconds to the current time.
				Date expirationDate = new Date(
						System.currentTimeMillis() + 3600000);
				GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(
						Constants.getPictureBucket(strVideo), Constants.PICTURE_NAME);
				urlRequest.setExpiration(expirationDate);
				urlRequest.setResponseHeaders(override);

				URL url = s3Client.generatePresignedUrl(urlRequest);

				Log.e("URL", url.toURI().toString());
				result.setUri(Uri.parse(url.toURI().toString()));

			} catch (Exception exception) {

				result.setErrorMessage(exception.getMessage());
			}

			return result;
		}

		protected void onPostExecute(S3TaskResult result) {
			
			if (result.getErrorMessage() != null) {

				displayErrorAlert(
						XMPPChatDemoActivity.this
								.getString(R.string.browser_failure_title),
						result.getErrorMessage());
			} else if (result.getUri() != null) {
				Log.e("HUHUHUHU",result.getUri().toString());
				
			}
		}
	}
	
	private class S3GenerateImagePresignedUrlTask extends AsyncTask<Void, Void, S3TaskResult> {
		
		protected S3TaskResult doInBackground(Void... voids) {

			S3TaskResult result = new S3TaskResult();

			try {
				// Ensure that the image will be treated as such.
				ResponseHeaderOverrides override = new ResponseHeaderOverrides();
				override.setContentType("image/png");

				// Generate the presigned URL.

				// Added an hour's worth of milliseconds to the current time.
				Date expirationDate = new Date(
						System.currentTimeMillis() + 3600000);
				GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(
						Constants.getPictureBucket(strVideo), imageName);
				urlRequest.setExpiration(expirationDate);
				urlRequest.setResponseHeaders(override);

				URL url = s3Client.generatePresignedUrl(urlRequest);

				Log.e("URL", url.toURI().toString());
				result.setUri(Uri.parse(url.toURI().toString()));

			} catch (Exception exception) {

				result.setErrorMessage(exception.getMessage());
			}

			return result;
		}

		protected void onPostExecute(S3TaskResult result) {
			
			if (result.getErrorMessage() != null) {

				displayErrorAlert(
						XMPPChatDemoActivity.this
								.getString(R.string.browser_failure_title),
						result.getErrorMessage());
			} else if (result.getUri() != null) {
				Log.e("HUHUHUHU",result.getUri().toString());
				//textMessage.setText(result.getUri().toString());
				
				String to = recipient.getText().toString();
				String text = "@image:"+ result.getUri().toString() + "@";
				Log.i("XMPPChatDemoActivity", "Sending text " + text + " to " + to);
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);
				if (connection != null) {
					connection.sendPacket(msg);
					messages.add(connection.getUser() + ":");
					messages.add(text);
					setListAdapter();
				}

			}
		}
	}

	private class S3TaskResult {
		String errorMessage = null;
		Uri uri = null;

		public String getErrorMessage() {
			return errorMessage;
		}

		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		public Uri getUri() {
			return uri;
		}

		public void setUri(Uri uri) {
			this.uri = uri;
		}
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
	
	protected void onActivityResult(int requestCode, int resultCode,
			Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

		switch (requestCode) {
		case PHOTO_SELECTED:
			if (resultCode == RESULT_OK) {
				Uri selectedImage = imageReturnedIntent.getData();
				Log.e("URI",selectedImage.toString());
				new S3PutObjectTask().execute(selectedImage);
			}
		}
	}

	public void pickupImage(Activity root){
		Intent i = new Intent(root, PickupImageActivity.class);
		root.startActivity(i);
	}
	
}