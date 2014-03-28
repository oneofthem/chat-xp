package com.demo.xmppchat;

import java.net.URL;
import java.util.Date;

import org.jivesoftware.smack.packet.Message;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;

public class S3GenerateImagePresignedUrlTask extends AsyncTask<Void, Void, S3TaskResult> {
	
	String imageName = "";
	private AmazonS3Client s3Client;
	Context context;
	public S3GenerateImagePresignedUrlTask(String imageName, AmazonS3Client s3Client, Context context){
		this.imageName = imageName;
		this.s3Client = s3Client;
		this.context = context;
	}
	
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
					Constants.getPictureBucket(Constants.strVideo), imageName);
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

	protected void displayErrorAlert(String title, String message) {

		AlertDialog.Builder confirm = new AlertDialog.Builder(context);
		confirm.setTitle(title);
		confirm.setMessage(message);

		confirm.setNegativeButton(context.getString(context.getResources().getIdentifier("ok", "string", context.getPackageName())),
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {

						((Activity)context).finish();
					}
				});

		confirm.show().show();
	}
	
	protected void onPostExecute(S3TaskResult result) {
		
		if (result.getErrorMessage() != null) {

			displayErrorAlert(
					context.getString(context.getResources().getIdentifier("browser_failure_title", "string", context.getPackageName())),
					result.getErrorMessage());
		} else if (result.getUri() != null) {
			Log.e("HUHUHUHU",result.getUri().toString());
			//textMessage.setText(result.getUri().toString());
			String to = Constants.receipient;
			if(to.equals(""))
				to = "test@sandbox-frienger.jinsei-iroiro.com";
			String text = "@image:"+ result.getUri().toString() + "@";
			Message msg = new Message(to, Message.Type.chat);
			msg.setBody(text);
			if (Constants.connection != null) {
				Constants.connection.sendPacket(msg);
				((Activity)context).finish();
//				messages.add(Constants.connection.getUser() + ":");
//				messages.add(text);
				//setListAdapter();
			}

		}
	}
}


