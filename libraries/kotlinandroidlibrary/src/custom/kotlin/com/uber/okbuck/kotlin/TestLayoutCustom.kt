package com.uber.okbuck.kotlin

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import com.uber.okbuck.kotlin.android.R
import kotlinx.android.synthetic.main.test_layout.view.*
import kotlinx.android.synthetic.custom.test_layout_custom.view.*

class TestLayoutCustom(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

  init {
    LayoutInflater.from(context).inflate(R.layout.test_layout, this)
    button.setText("test")
    LayoutInflater.from(context).inflate(R.layout.test_layout_custom, this)
    button_custom.setText("test_custom")
  }
}
