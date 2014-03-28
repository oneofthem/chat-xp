package com.demo.xmppchat;

import java.util.Locale;

import android.util.Log;

public class Constants {
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	
	public static final String ACCESS_KEY_ID = "AKIAJGKWTZ3EBK55SXLQ";
	public static final String SECRET_KEY = "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";
	
	public static final String PICTURE_BUCKET = "pchat";
	public static final String PICTURE_NAME = "Ishine";
	
	
	public static String getPictureBucket(String video) {
		return (video + ACCESS_KEY_ID + PICTURE_BUCKET).toLowerCase(Locale.JAPAN);
	}
	
}

