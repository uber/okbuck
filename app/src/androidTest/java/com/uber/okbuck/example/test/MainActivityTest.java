package com.uber.okbuck.example.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;

import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import com.uber.okbuck.example.MainActivity;
import com.uber.okbuck.example.R;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

  @Rule
  public ActivityTestRule<MainActivity> rule = new ActivityTestRule(MainActivity.class, true, true);

  @Test
  public void checkTextDisplayed() {
    onView(withId(R.id.mTextView2)).check(matches(withText("test in app")));
  }

  @Test
  public void java8LambdaCompilesAndWorksInInstrumentationApk() {
    Runnable runnable = () -> assertEquals(3, 2 + 1);
    runnable.run();
  }
}
