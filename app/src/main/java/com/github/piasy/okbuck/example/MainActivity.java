package com.github.piasy.okbuck.example;

import com.example.hellojni.HelloJni;
import com.github.piasy.okbuck.example.common.Calc;
import com.github.piasy.okbuck.example.common.CalcMonitor;
import com.github.piasy.okbuck.example.dummylibrary.DummyActivity;
import com.github.piasy.okbuck.example.dummylibrary.DummyAndroidClass;
import com.github.piasy.okbuck.example.javalib.DummyJavaClass;
import com.promegu.xlog.base.XLog;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.ButterKnife;

/**
 * Created by Piasy{github.com/Piasy} on 15/10/3.
 */
@XLog
public class MainActivity extends AppCompatActivity {
    private TextView mTextView;
    private TextView mTextView2;

    @Inject
    DummyJavaClass mDummyJavaClass;
    @Inject
    DummyAndroidClass mDummyAndroidClass;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bind();

        DummyComponent component = DaggerDummyComponent.builder().build();
        component.inject(this);

        mTextView.setText(String.format("%s %s, --from %s.", getString(
                        com.github.piasy.okbuck.example.dummylibrary.R.string.dummy_library_android_str),
                mDummyAndroidClass.getAndroidWord(this), mDummyJavaClass.getJavaWord()));

        mTextView2.setText(mTextView2.getText() + "\n\n" + HelloJni.stringFromJNI() + "\n\n" + FlavorLogger.log(this));

        // using explicit reference to cross module R reference:
        int id = android.support.design.R.string.appbar_scrolling_view_behavior;

        if (BuildConfig.CAN_JUMP) {
            mTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //startActivity(new Intent(MainActivity.this, CollapsingAppBarActivity.class));
                    startActivity(new Intent(MainActivity.this, DummyActivity.class));
                }
            });
        }

        Log.d("test", "1 + 2 = " + new Calc(new CalcMonitor(this)).add(1, 2));
    }

    private void bind() {
        mTextView = ButterKnife.findById(this, R.id.mTextView);
        mTextView2 = ButterKnife.findById(this, R.id.mTextView2);
    }
}
