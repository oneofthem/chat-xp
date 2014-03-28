package com.demo.xmppchat;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class PickupImageActivity extends Activity {

	private static final int PHOTO_SELECTED = 1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(intent, PHOTO_SELECTED);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case PHOTO_SELECTED:
			if (resultCode == RESULT_OK) {
				Uri selectedImage = data.getData();
				Log.e("URI",selectedImage.toString());
				new S3PutObjectTask(this).execute(selectedImage);
			}
		}
	}


}
