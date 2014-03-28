package com.demo.xmppchat;

import android.net.Uri;

public class S3TaskResult {
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
