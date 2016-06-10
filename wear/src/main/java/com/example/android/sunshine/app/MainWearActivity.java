package com.example.android.sunshine.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainWearActivity extends WearableActivity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String LOG_TAG = "MainWearActivity";
    private static final String WEARABLE_DATA_PATH = "/wearable_weather_data";
    private static final String DATA_ITEM_NAME_LOW_TEMP = "lowTemp";
    private static final String DATA_ITEM_NAME_HIGH_TEMP = "highTemp";
    private static final String DATA_ITEM_NAME_WEATHER_ICON = "weatherIcon";

    private static final int TIMEOUT_MS = 5000;

    private GoogleApiClient mGoogleApiClient;

    private static final SimpleDateFormat DISPLAY_DATE_FORMAT =
            new SimpleDateFormat("EEE, MMM d, yyyy", Locale.US);

    private static final SimpleDateFormat DISPLAY_TIME_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTimeView;
    private TextView mDateView;
    private TextView mLowTemp;
    private TextView mHighTemp;
    private ImageView mWeatherIcon;
    private LinearLayout mTempView;

    private int lowTemp = Integer.MIN_VALUE;
    private int highTemp = Integer.MAX_VALUE;
    private Bitmap weatherIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mTimeView = (TextView) findViewById(R.id.time);
        mDateView = (TextView) findViewById(R.id.date);
        mTempView = (LinearLayout) findViewById(R.id.temp);
        mLowTemp = (TextView) findViewById(R.id.low_temp);
        mHighTemp = (TextView) findViewById(R.id.high_temp);
        mWeatherIcon = (ImageView) findViewById(R.id.weather_icon);
        setDateTime();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(LOG_TAG, "onConnected.");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "onConnectionSuspended. i: " + i);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(LOG_TAG, "onDataChanged.");
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().equals(WEARABLE_DATA_PATH)) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    lowTemp = dataMap.getInt(DATA_ITEM_NAME_LOW_TEMP);
                    highTemp = dataMap.getInt(DATA_ITEM_NAME_HIGH_TEMP);
                    Asset weatherIconAsset = dataMap.getAsset(DATA_ITEM_NAME_WEATHER_ICON);
                    loadBitmapFromAsset(weatherIconAsset);

                    Log.d(LOG_TAG, "lowTemp: " + lowTemp);
                    Log.d(LOG_TAG, "highTemp: " + highTemp);
                    Log.d(LOG_TAG, "weatherIcon: " + weatherIcon);
                }
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "onConnectionFailed. " + connectionResult.getErrorCode());
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTimeView.setVisibility(View.VISIBLE);
            setDateTime();
            if (lowTemp != Integer.MIN_VALUE) {
                mLowTemp.setText(lowTemp);
            }
            if (highTemp != Integer.MAX_VALUE) {
                mHighTemp.setText(highTemp);
            }
            if (weatherIcon != null) {
                mWeatherIcon.setImageBitmap(weatherIcon);
            }
        } else {
            mContainerView.setBackground(null);
            mTimeView.setTextColor(getResources().getColor(android.R.color.black));
            mDateView.setVisibility(View.GONE);
            mTempView.setVisibility(View.GONE);
        }
    }

    private void setDateTime() {
        Date now = new Date();
        mTimeView.setText(DISPLAY_TIME_FORMAT.format(now));
        mDateView.setText(DISPLAY_DATE_FORMAT.format(now));
    }

    private void loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        new LoadBitmapFromAssetAsyncTask().execute(asset);
    }

    private class LoadBitmapFromAssetAsyncTask extends AsyncTask<Asset, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Asset... params) {
            ConnectionResult result =
                    mGoogleApiClient.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!result.isSuccess()) {
                return null;
            }
            // convert asset into a file descriptor and block until it's ready
            InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                    mGoogleApiClient, params[0]).await().getInputStream();
            mGoogleApiClient.disconnect();

            if (assetInputStream == null) {
                Log.w(LOG_TAG, "Requested an unknown Asset.");
                return null;
            }
            // decode the stream into a bitmap
            return BitmapFactory.decodeStream(assetInputStream);
        }

        @Override
        protected void onPostExecute(Bitmap resultBitmap) {
            weatherIcon = resultBitmap;
        }
    }
}
