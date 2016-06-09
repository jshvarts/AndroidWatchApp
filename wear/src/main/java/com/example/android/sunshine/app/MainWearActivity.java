package com.example.android.sunshine.app;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainWearActivity extends WearableActivity {

    private static final SimpleDateFormat DISPLAY_DATE_FORMAT =
            new SimpleDateFormat("EEE, MMM d, yyyy", Locale.US);

    private static final SimpleDateFormat DISPLAY_TIME_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTimeView;
    private TextView mDateView;
    private LinearLayout mTempView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTimeView = (TextView) findViewById(R.id.time);
        mDateView = (TextView) findViewById(R.id.date);
        mTempView = (LinearLayout) findViewById(R.id.temp);
        setDateTime();
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

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTimeView.setVisibility(View.VISIBLE);
            setDateTime();
        } else {
            mContainerView.setBackground(null);
            mTimeView.setTextColor(getResources().getColor(android.R.color.black));
            mDateView.setVisibility(View.GONE);
            mTempView.setVisibility(View.GONE);
        }
    }

    private void setDateTime() {
        Calendar cal = Calendar.getInstance(Locale.US);
        mTimeView.setText(DISPLAY_TIME_FORMAT.format(cal.getTime()));
        mDateView.setText(DISPLAY_DATE_FORMAT.format(cal.getTime()));
    }
}
