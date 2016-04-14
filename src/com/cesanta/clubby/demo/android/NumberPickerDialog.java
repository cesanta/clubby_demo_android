
package com.cesanta.clubby.demo.android;

import android.app.AlertDialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Bundle;
import android.view.Gravity;


import android.widget.LinearLayout;
import android.widget.NumberPicker;

public class NumberPickerDialog extends AlertDialog implements DialogInterface.OnClickListener {

    private NumberPicker mNumberPicker;
    private ValueChangeListener mValueChangeListener;
    private int mValue;

    public NumberPickerDialog(Context context, ValueChangeListener valueChangeListener, int value, String title, String unit) {
        super(context);

        mValueChangeListener = valueChangeListener;
        mValue = value;

        Context c = getContext();
        setTitle(title);

        if (unit != null) {
            setTitle(title + ", " + unit);
        }

        {
            LinearLayout ll = new LinearLayout(context);
            ll.setGravity(Gravity.CENTER);

            mNumberPicker = new NumberPicker(context);
            mNumberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    mValue = newVal;
                }
            });
            ll.addView(mNumberPicker);
            setView(ll);
        }

        setButton( BUTTON_POSITIVE, c.getString( R.string.ok ), this);
        setButton( BUTTON_NEGATIVE, c.getString( R.string.cancel ), this );
    }

    public void setMin(int min) {
        mNumberPicker.setMinValue(min);
    }

    public void setMax(int max) {
        mNumberPicker.setMaxValue(max);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNumberPicker.setValue(mValue);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON_POSITIVE && mValueChangeListener != null) {
            mNumberPicker.clearFocus();
            mValueChangeListener.valueChanged(mNumberPicker.getValue());
        }
    }

    public interface ValueChangeListener {
        public void valueChanged(int value);
    }
}
