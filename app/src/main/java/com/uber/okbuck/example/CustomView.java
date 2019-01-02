package com.uber.okbuck.example;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class CustomView extends AppCompatTextView {
  public CustomView(Context context) {
    super(context);
  }

  public CustomView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public CustomView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }
}
