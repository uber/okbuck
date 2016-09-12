package com.github.okbuilds.okbuck.example.test;

import android.support.test.rule.ActivityTestRule;

import com.github.okbuilds.okbuck.example.MainActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.okbuilds.okbuck.example.R;
import android.support.test.runner.AndroidJUnit4;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> rule =
            new ActivityTestRule(MainActivity.class, true, true);

    @Test
    public void checkTextDisplayed() {
        onView(withId(R.id.mTextView2)).check(matches(withText("test in app")));
    }
}
