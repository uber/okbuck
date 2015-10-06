package com.github.piasy.okbuck.example;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.github.piasy.okbuck.example.dummylibrary.DummyAndroidClass;
import com.github.piasy.okbuck.example.javalib.DummyJavaClass;
import com.promegu.xlog.base.XLog;
import javax.inject.Inject;

/**
 * Created by Piasy{github.com/Piasy} on 15/10/3.
 */
@XLog
public class MainActivity extends AppCompatActivity {
    TextView mTextView;

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

        // using explicit reference to cross module R reference:
        int id = android.support.design.R.string.appbar_scrolling_view_behavior;

        if (BuildConfig.CAN_JUMP) {
            mTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, CollapsingAppBarActivity.class));
                }
            });
        }
    }

    private void bind() {
        mTextView = ButterKnife.findById(this, R.id.mTextView);
    }
}
