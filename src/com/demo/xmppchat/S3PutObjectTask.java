package com.demo.xmppchat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class S3PutObjectTask extends AsyncTask<Uri, Void, S3TaskResult> {
	
	Context context;
	ProgressDialog dialog;
	long startTime;
	String imageName = "";
	String strVideo = "pchatupload";
	private AmazonS3Client s3Client = new AmazonS3Client(
			new BasicAWSCredentials(Constants.ACCESS_KEY_ID,
					Constants.SECRET_KEY));
	public S3PutObjectTask(Context context){
		this.context = context;
	}
	
	protected void onPreExecute() {
		dialog = new ProgressDialog(context);
		dialog.setMessage(context.getResources().getString(context.getResources().getIdentifier("uploading", "string", context.getPackageName())));
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

		Cursor cursor = context.getContentResolver().query(selectedImage,
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

		dialog.dismiss();
		long time = System.currentTimeMillis() - startTime;
		Log.e("TIME UPLOAD",time+"");
		if (result.getErrorMessage() != null) {
			displayErrorAlert(
					context.getString(R.string.upload_failure_title),
					result.getErrorMessage());
		}else{
			//new S3GeneratePresignedUrlTask().execute();
			new S3GenerateImagePresignedUrlTask(this.imageName, this.s3Client, this.context).execute();
		}
	}
}

