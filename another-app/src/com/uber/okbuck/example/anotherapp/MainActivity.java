package com.uber.okbuck.example.anotherapp;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.hellojni.HelloJni;
import com.uber.okbuck.example.common.Calc;
import com.uber.okbuck.example.common.CalcMonitor;

public class MainActivity extends AppCompatActivity {

  private TextView mTvTest;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    CalcMonitor calcMonitor = new CalcMonitor(this);
    calcMonitor.addCalled("from another-app");

    mTvTest = findViewById(R.id.mTvTest);
    mTvTest.setOnClickListener(v -> mTvTest());
  }

  void mTvTest() {
    Log.d("TEST", "" + androidx.appcompat.R.color.button_material_light);
    mTvTest.setText(
        new StringBuilder()
            .append("1 + 2 = ")
            .append(new Calc(new CalcMonitor(this)).add(1, 2))
            .append("\n\n")
            .append(HelloJni.stringFromJNI())
            .append("\n\n")
            .append(getString(com.uber.okbuck.example.empty.R.string.empty_release_string))
            .toString());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
