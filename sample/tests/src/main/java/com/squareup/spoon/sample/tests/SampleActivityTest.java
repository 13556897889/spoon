package com.squareup.spoon.sample.tests;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.TextView;
import com.squareup.spoon.Screenshot;
import com.squareup.spoon.sample.R;
import com.squareup.spoon.sample.SampleActivity;

import java.util.Random;

public class SampleActivityTest extends ActivityInstrumentationTestCase2<SampleActivity> {
  public SampleActivityTest() {
    super(SampleActivity.class);
  }

  private Instrumentation instrumentation;
  private SampleActivity activity;

  @Override protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activity = getActivity();
  }

  public void testClickButtonOneDisplaysHelloOne() throws InterruptedException {
    final Button button = (Button) activity.findViewById(R.id.click_me1);
    TextView textView = (TextView) activity.findViewById(R.id.say_hello);

    Screenshot.snap(activity, "initial_state");
    // Check initial state.
    assertEquals("", textView.getText());
    assertEquals(activity.getString(R.string.click_me1), button.getText());

    // Click the button to change state.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        button.performClick();
      }
    });

    // Check new state.
    assertEquals(activity.getString(R.string.hello1), textView.getText());
    Screenshot.snap(activity, "after_button_press");
  }

  public void testClickButtonTwoDisplaysHelloTwo() throws InterruptedException {
    final Button button = (Button) activity.findViewById(R.id.click_me2);
    TextView textView = (TextView) activity.findViewById(R.id.say_hello);

    Screenshot.snap(activity, "initial_state");
    // Check initial state.
    assertEquals("", textView.getText());
    assertEquals(activity.getString(R.string.click_me2), button.getText());

    // Click the button to change state.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        button.performClick();
      }
    });

    // Check new state.
    assertEquals(activity.getString(R.string.hello2), textView.getText());
    Screenshot.snap(activity, "after_button_press");

    randomlyFail();
  }

  public void testClickButtonThreeDisplaysHelloThree() throws InterruptedException {
    final Button button = (Button) activity.findViewById(R.id.click_me3);
    TextView textView = (TextView) activity.findViewById(R.id.say_hello);

    Screenshot.snap(activity, "initial_state");
    // Check initial state.
    assertEquals("", textView.getText());
    assertEquals(activity.getString(R.string.click_me3), button.getText());

    // Click the button to change state.
    instrumentation.runOnMainSync(new Runnable() {
      @Override public void run() {
        button.performClick();
      }
    });

    // Check new state.
    assertEquals(activity.getString(R.string.hello3), textView.getText());
    Screenshot.snap(activity, "after_button_press");

    randomlyFail();
  }

  public void testAlwaysFailingForFun() {
    Screenshot.snap(activity, "initial_state");
    fail("Whoops!");
  }

  private static final Random RANDOM = new Random();
  private static void randomlyFail() {
    assertTrue(RANDOM.nextInt(3) > 0);
  }
}
