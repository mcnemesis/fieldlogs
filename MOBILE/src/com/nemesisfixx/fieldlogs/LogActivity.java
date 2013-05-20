package com.nemesisfixx.fieldlogs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;


import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class LogActivity extends Activity {

	public final String Tag = "FIELDLOGS";
	public final static String API_URL = "http://logbots.mobi/logs/";
	protected static final String API_POST_FIELDLOG_PATH = "api/logs/add";
	private String dataPath;
	private Handler handler;

	private class FieldLog {
		public String barcode, subject, birthdate, latitude, longitude,
				location, nationality, photocaption, sig_path, photo_path;
		public String comment;
	}

	private enum ActiveIntent {
		PHOTO, SIGNATURE, BARCODE, NONE
	}

	ActiveIntent activeIntent;
	FieldLog fieldLog = new FieldLog();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);

		handler = new Handler();

		try {
			dataPath = Utility
					.createSDCardDir(getString(R.string.image_directory));
		} catch (Exception e) {
			Log.e(Tag, "Image Path Error : " + e.getMessage());
			Utility.showToast(e.getMessage(), getApplicationContext(),
					Toast.LENGTH_LONG);
		}

		// take a photo
		ImageButton photoButton = (ImageButton) findViewById(R.id.imgPhoto);

		photoButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				obtainCameraImage();

			}
		});

		// capture the signature
		ImageButton sigButton = (ImageButton) findViewById(R.id.imgSignature);

		sigButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				obtainRecipientSignature();

			}
		});

		// get the barcode
		ImageButton btnBarCode = (ImageButton) findViewById(R.id.btnBarCode);

		btnBarCode.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				scanBarcode();

			}
		});

		// setup GPS Shit...
		configureGPS();

		// submit the stuff
		ImageButton btnSubmitLog = (ImageButton) findViewById(R.id.btnSubmit);

		btnSubmitLog.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				submitCurrentLog();

			}
		});

	}

	protected void submitCurrentLog() {

		parseCurrentFieldLog();

		if (validateLog(fieldLog)) {
			postFieldLog(fieldLog);
		}

	}

	private void parseCurrentFieldLog() {
		EditText txtSubject = (EditText) findViewById(R.id.txtSubject);
		fieldLog.subject = txtSubject.getText().toString();

		Spinner spinNationality = (Spinner) findViewById(R.id.spinNationality);
		fieldLog.nationality = getResources().getStringArray(
				R.array.nationalities)[spinNationality
				.getSelectedItemPosition()];

		DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);
		fieldLog.birthdate = Utility.parseDate(datePicker);

		EditText txtLocation = (EditText) findViewById(R.id.txtLocation);
		fieldLog.location = txtLocation.getText().toString();

		EditText txtComment = (EditText) findViewById(R.id.txtComment);
		fieldLog.comment = txtComment.getText().toString();

		EditText txtCaption = (EditText) findViewById(R.id.txtCaption);
		fieldLog.photocaption = txtCaption.getText().toString();
	}

	private void postFieldLog(final FieldLog log) {

		if (!Utility.isNetworkAvailable(this)) {
			Utility.showAlert("Error While Posting...",
					"~ NO Internet Connection ~", R.drawable.ic_error,
					LogActivity.this);
			Log.i(Tag, "~ NO Internet Connection ~");

			return;
		}

		disablePostingButton();

		(new Thread(new Runnable() {
			public void run() {

				List<NameValuePair> httpPostParams = new ArrayList<NameValuePair>();

				// add gps location data to collection
				httpPostParams.add(new BasicNameValuePair("code", log.barcode));
				httpPostParams.add(new BasicNameValuePair("cap",
						log.photocaption));
				httpPostParams.add(new BasicNameValuePair("com", log.comment));
				httpPostParams.add(new BasicNameValuePair("loc", log.location));
				httpPostParams
						.add(new BasicNameValuePair("dob", log.birthdate));
				httpPostParams.add(new BasicNameValuePair("nat",
						log.nationality));
				httpPostParams.add(new BasicNameValuePair("sub", log.subject));

				httpPostParams.add(new BasicNameValuePair("lat", log.latitude));
				httpPostParams
						.add(new BasicNameValuePair("lng", log.longitude));

				List<NameValuePair> files = new ArrayList<NameValuePair>();

				files.add(new BasicNameValuePair("sig", log.sig_path));
				files.add(new BasicNameValuePair("img", log.photo_path));

				final HttpResponse response = Utility.postHTTP(
						make_api_callpath(LogActivity.API_POST_FIELDLOG_PATH),
						httpPostParams, files);

				if (response.getStatusLine().getStatusCode() == 200) { // success
					// on ui thread
					handler.post(new Runnable() {
						public void run() {

							Utility.showAlert("Posting Field Logs...",
									"Log Submission Successful!",
									R.drawable.ic_correct, LogActivity.this);
							Log.i(Tag, "Submission Successful");

							try {
								Log.i(Tag, EntityUtils.toString(response
										.getEntity()));
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							enablePostingButton();
						}
					});
				} else {
					// on ui thread
					handler.post(new Runnable() {
						public void run() {
							Utility.showAlert("Posting Field Logs...",
									"Log Submission Failed.", R.drawable.ic_error,
									LogActivity.this);
							try {
								Log.e(Tag,
										String.format("Server : %s | %s | %s",
												response.getStatusLine()
														.getStatusCode(),
												response.getStatusLine()
														.getReasonPhrase(),
												EntityUtils.toString(response
														.getEntity())));
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							Log.e(Tag, "Submission Failed!");

							enablePostingButton();

						}
					});
				}
			}
		})).start();

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem v) {
		switch (v.getItemId()) {

		case R.id.menu_about: {
			showAbout();
			return true;
		}
		}

		return false;
	}

	private void showAbout() {
		Utility.showAlert(
				String.format("About %s", getString(R.string.app_name)),
				"A Partial-Generic data collection utility.\n\nDeveloped by Nemesis Fixx for enumer8.",
				R.drawable.ic_launcher, this);
	}

	public String make_api_callpath(String path) {
		return String.format("%s%s", LogActivity.API_URL, path);
	}

	private void disablePostingButton() {
		View btn = (View) findViewById(R.id.btnSubmit);
		btn.setVisibility(View.INVISIBLE);
	}

	private void enablePostingButton() {
		View btn = (View) findViewById(R.id.btnSubmit);
		btn.setVisibility(View.VISIBLE);
	}

	private boolean strIsNullOrEmpty(String str) {
		return ((str == null) || (str.trim().length() == 0)) ? true : false;
	}

	private boolean validateLog(FieldLog log) {

		if (strIsNullOrEmpty(log.latitude)) {
			Utility.showAlert("Input Error",
					"Please obtain GPS before submitting.", this);
			return false;
		}

		if (strIsNullOrEmpty(log.longitude)) {
			Utility.showAlert("Input Error",
					"Please obtain GPS before submitting.", this);
			return false;
		}

		if (strIsNullOrEmpty(log.subject)) {
			Utility.showAlert("Input Error", "A Subject is required", this);
			return false;
		}

		if (strIsNullOrEmpty(log.birthdate)) {
			Utility.showAlert("Input Error",
					"Please specify the Date of Birth", this);
			return false;
		}

		if (strIsNullOrEmpty(log.nationality)) {
			Utility.showAlert("Input Error", "Please specify the Nationality",
					this);
			return false;
		}

		if (strIsNullOrEmpty(log.nationality)) {
			Utility.showAlert("Input Error", "Please specify the Nationality",
					this);
			return false;
		}

		if (strIsNullOrEmpty(log.sig_path)) {
			Utility.showAlert("Input Error", "A Signature is required", this);
			return false;
		}

		return true;
	}

	private void configureGPS() {
		LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// mlocManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
		// 60000,
		mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
				60000, 0, new LocationListener() {

					public void onLocationChanged(Location location) {

						LogActivity.this.fieldLog.latitude = String.format(
								"%s", location.getLatitude());
						LogActivity.this.fieldLog.longitude = String.format(
								"%s", location.getLongitude());

						try {
							((TextView) findViewById(R.id.lblGPSStatus)).setText(String
									.format("Fresh GPS : %s,%s",
											LogActivity.this.fieldLog.latitude,
											LogActivity.this.fieldLog.longitude));

						} catch (Exception e) {

						}

					}

					public void onProviderDisabled(String provider) {
						Utility.showToast("GPS has been Disabled",
								getApplicationContext());

						try {
							((TextView) findViewById(R.id.lblGPSStatus))
									.setText(getString(R.string.default_gps_status));

						} catch (Exception e) {

						}

					}

					public void onProviderEnabled(String provider) {
						Utility.showToast("GPS has been Enabled",
								getApplicationContext());
					}

					public void onStatusChanged(String provider, int status,
							Bundle extras) {
						// nothing to do
					}

				});

	}

	protected void scanBarcode() {
		Intent intent = new Intent("com.google.zxing.client.android.SCAN");
		intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // limit to only QR-Codes
		startActivityForResult(intent, ActiveIntent.BARCODE.ordinal());
		activeIntent = ActiveIntent.BARCODE;
	}

	protected void obtainRecipientSignature() {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.signature_layout);
		dialog.setTitle(R.string.lbl_signature);
		dialog.setCancelable(true);

		final SignatureView sigView = (SignatureView) dialog
				.findViewById(R.id.signatueView);

		// set up button
		Button finishedBtn = (Button) dialog
				.findViewById(R.id.btnSigningComplete);
		finishedBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Bitmap bitmap = sigView.toBitMap();
				fieldLog.sig_path = getNewImagePath("recipient_signature");
				Utility.saveBitmapToFile(bitmap, fieldLog.sig_path, 50);

				ImageView img = (ImageView) findViewById(R.id.imgSignature);

				File imgFile = new File(fieldLog.sig_path);
				if (imgFile.exists()) {

					setThumbnailImage(img, imgFile);

				}

				dialog.dismiss();
			}
		});

		// clearbutton
		Button clearBtn = (Button) dialog.findViewById(R.id.btnClearSignature);
		clearBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sigView.clear();
			}
		});

		// now that the dialog is set up, it's time to show it
		dialog.show();
	}

	public void setThumbnailImage(ImageView imgView, File imgFile) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 2;
		Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(),
				options);

		imgView.setImageBitmap(bitmap);
		imgView.setAdjustViewBounds(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_log, menu);
		return true;
	}

	private String getNewImagePath(String root) {
		return String.format("%s/%s_%s", dataPath, root, (new Date()).getTime()
				+ ".jpg");
	}

	private void obtainCameraImage() {

		fieldLog.photo_path = getNewImagePath("log_photos");

		Log.i(Tag, "Creating Image : " + fieldLog.photo_path);

		try {
			activeIntent = ActiveIntent.PHOTO;
			startCameraActivity(fieldLog.photo_path);
		} catch (Exception e) {
			Log.i(Tag, "Camera Error : " + e.getMessage());
		}
		return;
	}

	protected void startCameraActivity(String path) {
		File file = new File(path);
		Uri outputFileUri = Uri.fromFile(file);

		Intent intent = new Intent(
				android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

		startActivityForResult(intent, activeIntent.ordinal());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {

		if ((activeIntent == ActiveIntent.BARCODE)
				&& (requestCode == ActiveIntent.BARCODE.ordinal())) {
			if (resultCode == RESULT_OK) {

				String contents = intent.getStringExtra("SCAN_RESULT");
				String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

				Log.i(Tag, String.format("Bar-Code Scan : %s (%s)", contents,
						format));

				saveAndShowBarCode(contents);
			}

		} else if ((activeIntent == ActiveIntent.PHOTO)
				&& (requestCode == ActiveIntent.PHOTO.ordinal())) {

			if (resultCode == RESULT_OK) {
				onPhotoTaken(fieldLog.photo_path, R.id.imgPhoto,
						R.drawable.camera);
			}
		}
	}

	private void saveAndShowBarCode(String contents) {

		TextView barCodeStatus = (TextView) findViewById(R.id.lblBarCode);

		barCodeStatus.setText(String.format("%s\n---\nCURRENT: %s",
				getString(R.string.default_barcode), contents));

		fieldLog.barcode = contents;
	}

	protected void onPhotoTaken(String imgPath, int imageView,
			int defaultResourceImage) {
		try {

			Log.d(Tag, "Loading image now...");

			File imgFile = new File(imgPath);
			if (imgFile.exists()) {

				setThumbnailImageAndSave((ImageView) findViewById(imageView),
						imgFile, 25);

			}

		} catch (Exception e) {
			Log.e(Tag, "Image Error : " + e.getMessage());
		}

		activeIntent = ActiveIntent.NONE;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		try {
			File imgFile = new File(fieldLog.photo_path);
			if (imgFile.exists())
				Utility.DeleteFile(fieldLog.photo_path);

		} catch (Exception e) {
		}

		try {
			File imgFile = new File(fieldLog.sig_path);
			if (imgFile.exists())
				Utility.DeleteFile(fieldLog.sig_path);

		} catch (Exception e) {
		}

	}

	public void setThumbnailImageAndSave(ImageView imgView, File imgFile,
			int qualityPercent) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = (int) Math.floor(qualityPercent / 100.0);
		Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(),
				options);

		imgView.setImageBitmap(bitmap);
		imgView.setAdjustViewBounds(true);

		Utility.saveBitmapToFile(bitmap, imgFile.getAbsolutePath(),
				qualityPercent);
	}

}
