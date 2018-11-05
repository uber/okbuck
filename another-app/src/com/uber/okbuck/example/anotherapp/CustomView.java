package com.uber.okbuck.example.anotherapp;

import android.content.Context;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;

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
