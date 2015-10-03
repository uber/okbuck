package com.github.piasy.okbuck.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import butterknife.ButterKnife;

/**
 * Created by Piasy{github.com/Piasy} on 15/10/3.
 */
public class MainActivity extends AppCompatActivity {
    TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bind();

        mTextView.setText(R.string.app_name);
    }

    private void bind() {
        mTextView = ButterKnife.findById(this, R.id.mTextView);
    }
}
