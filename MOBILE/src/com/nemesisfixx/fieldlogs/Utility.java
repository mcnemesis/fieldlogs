package com.nemesisfixx.fieldlogs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

public class Utility {

	final static String Tag = "trax";
	private static final long DEFAULT_VIBRATE_TIME = 300;
	public static boolean isPosting = false;

	public static String parseDate(DatePicker datepicker) {
		Date date = new Date(datepicker.getYear() - 1900,
				datepicker.getMonth(), datepicker.getDayOfMonth());
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(date);
	}

	public static String parseTime(TimePicker timePicker) {
		String time = null;

		time = String.format("%s:%s", timePicker.getCurrentHour(),
				timePicker.getCurrentMinute());

		return time;
	}

	/*
	 * Will create directory on the External Storage Card with the given dirName
	 * name.
	 * 
	 * Throws an exception is dirName is null, and returns the name of the
	 * created directory if successful
	 */
	public static String createSDCardDir(String dirName) throws Exception {

		Log.d(Tag, "Creating Dir on sdcard...");

		if (dirName == null)
			throw new Exception("dirName must not be null");

		File folder = new File(String.format("%s/%s",
				Environment.getExternalStorageDirectory(), dirName));

		boolean success = false;

		if (!folder.exists()) {
			success = folder.mkdir();
			Log.d(Tag, "Created Dir on sdcard...");
		} else {
			success = true;
			Log.d(Tag, "Dir exists on sdcard...");
		}

		if (success) {
			return folder.getAbsolutePath();
		} else {
			Log.d(Tag, "Failed to create on sdcard...");
			throw new Exception(
					"Failed to create directory on External SD Card");
		}
	}

	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		// if no network is available networkInfo will be null, otherwise check
		// if we are connected
		if (networkInfo != null && networkInfo.isConnected()) {
			return true;
		}
		return false;
	}

	public static String getHTTP(String url) throws ClientProtocolException,
			IOException {
		String result = "";
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		ResponseHandler<String> responseHandler = new BasicResponseHandler();

		// Execute HTTP Get Request
		result = httpclient.execute(httpget, responseHandler);
		return result;
	}

	public static HttpResponse postHTTP(String url,
			List<NameValuePair> nameValuePairs, List<NameValuePair> files) {
		if (isPosting)
			return null;

		try {

			MultipartEntity entity = new MultipartEntity(
					HttpMultipartMode.BROWSER_COMPATIBLE);

			// add normal post data
			for (int index = 0; index < nameValuePairs.size(); index++) {
				// Normal string data
				if (nameValuePairs.get(index).getValue() != null) {
					entity.addPart(
							nameValuePairs.get(index).getName(),
							new StringBody(nameValuePairs.get(index).getValue()));
				}
			}

			// add files
			for (int index = 0; index < files.size(); index++) {

				if (files.get(index).getValue() != null) {
					File file = new File(files.get(index).getValue());

					if (file.exists()) {
						entity.addPart(files.get(index).getName(),
								new FileBody(file));
					} else {
						Log.e(Tag, "POST FILE Doesn't exist! " + file.getName());
					}
				}
			}

			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			HttpPost httpPost = new HttpPost(url);
			httpPost.getParams().setParameter(
					CoreProtocolPNames.USE_EXPECT_CONTINUE, Boolean.FALSE);

			httpPost.setEntity(entity);

			HttpResponse res = httpClient.execute(httpPost, localContext);

			isPosting = false;

			return res;

		} catch (IOException e) {
			Log.i(Tag, "Error : " + e.getMessage());
			e.printStackTrace();
			isPosting = false;
			return null;
		}
	}

	public static boolean DeleteFile(String path) {
		File file = new File(path);
		return file.delete();
	}

	/*
	 * Display a toast with the default duration : Toast.LENGTH_SHORT
	 */
	public static void showToast(String message, Context context) {
		showToast(message, context, Toast.LENGTH_SHORT);
	}

	/*
	 * Display a toast with given Duration
	 */
	public static void showToast(String message, Context context, int duration) {
		Toast.makeText(context, message, duration).show();
	}

	public static void showAlert(String title, String message, Context context) {
		showAlert(title, message, R.drawable.ic_launcher, context);
	}

	public static void showAlert(String title, String message, int iconId,
			Context context) {
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);

			builder.setIcon(iconId);
			builder.setTitle(title);
			builder.setMessage(message);
			// builder.setCancelable(false);

			AlertDialog alert = builder.create();
			alert.show();
		} catch (Exception e) {
			Log.e(Tag, "Alert Error : " + e.getMessage());
		}
	}

	public static void vibrate(Context context) {
		Vibrator v = (Vibrator) context
				.getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(DEFAULT_VIBRATE_TIME);
	}

	public static void saveBitmapToFile(Bitmap bitmap, String filename,
			int qualityPercent) {
		try {
			FileOutputStream out = new FileOutputStream(filename);
			bitmap.compress(Bitmap.CompressFormat.JPEG, qualityPercent, out);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String parseDateTime(DatePicker dateP, TimePicker timeP) {
		return String.format("%s %s", parseDate(dateP), parseTime(timeP));
	}

}
