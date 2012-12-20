package com.squareup.spoon;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.synchronizedList;
import static java.util.Collections.synchronizedMap;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

public class ExecutionSummary {
  static final ThreadLocal<DateFormat> DISPLAY_TIME = new ThreadLocal<DateFormat>() {
    @Override protected DateFormat initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd hh:mm a");
    }
  };

  /** Assets which need to be copied to the output directory when generating HTML. */
  private static final String[] ASSETS = {
      "bootstrap.min.css", "bootstrap.min.js", "jquery.min.js", "lightbox.js", "spoon.css",
      "lightbox.css", "loading.gif", "next.png", "prev.png", "close.png",
      "jquery-ui-1.8.18.custom.min.js", "jquery.smooth-scroll.min.js"
  };

  private final File output;
  private final String title;
  private final List<ExecutionResult> results;
  private final Map<String, InstrumentationTestClass> instrumentationTestClasses;
  private final Exception exception;
  private final long totalTime;
  private final Calendar started;
  private final Calendar ended;
  private final int totalTests;
  private final int totalSuccess;
  private final int totalFailure;
  private final String displayTime;

  public ExecutionSummary(File output, String title, List<ExecutionResult> results,
      Map<String, InstrumentationTestClass> testClasses, Exception exception, long totalTime,
      Calendar started, Calendar ended, int totalTests, int totalSuccess, int totalFailure,
      String displayTime) {
    this.output = output;
    this.title = title;
    this.results = unmodifiableList(results);
    this.instrumentationTestClasses = unmodifiableMap(testClasses);
    this.exception = exception;
    this.totalTime = totalTime;
    this.started = started;
    this.ended = ended;
    this.totalTests = totalTests;
    this.totalSuccess = totalSuccess;
    this.totalFailure = totalFailure;
    this.displayTime = displayTime;
  }

  public String getTitle() {
    return title;
  }

  public List<ExecutionResult> getResults() {
    return results;
  }

  public Collection<InstrumentationTestClass> getInstrumentationTestClasses() {
    return instrumentationTestClasses.values();
  }

  public Exception getException() {
    return exception;
  }

  /** Total execution time (in seconds). */
  public long getTotalTime() {
    return totalTime;
  }

  public Calendar getStarted() {
    return started;
  }

  public Calendar getEnded() {
    return ended;
  }

  public int getTotalTests() {
    return totalTests;
  }

  public int getTotalSuccess() {
    return totalSuccess;
  }

  public int getTotalFailure() {
    return totalFailure;
  }

  public String getDisplayTime() {
    return displayTime;
  }

  public void writeHtml() {
    for (String asset : ASSETS) {
      copyResourceToOutput(asset, output);
    }

    DefaultMustacheFactory mustacheFactory = new DefaultMustacheFactory();
    Mustache summary = mustacheFactory.compile("index.html");
    Mustache device = mustacheFactory.compile("index-device.html");
    Mustache testClass = mustacheFactory.compile("index-test-class.html");
    Mustache test = mustacheFactory.compile("index-test.html");
    try {
      summary.execute(new FileWriter(new File(output, "index.html")), this).flush();

      for (ExecutionResult result : results) {
        result.updateDynamicValues();

        File deviceOutput = new File(output, result.serial + "/index.html");
        device.execute(new FileWriter(deviceOutput), result).flush();
      }

      for (InstrumentationTestClass instrumentationTestClass : getInstrumentationTestClasses()) {
        File testDir = new File(output, instrumentationTestClass.classSimpleName);
        testDir.mkdirs();
        File testClassOutput = new File(testDir.getPath(), "index.html");
        testClass.execute(new FileWriter(testClassOutput), instrumentationTestClass).flush();

        for (InstrumentationTest instrumentationTest : instrumentationTestClass.tests()) {
          File testOutput = new File(testDir.getPath(), instrumentationTest.testName + ".html");
          test.execute(new FileWriter(testOutput), instrumentationTest).flush();
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to write output HTML.", e);
    }
  }

  private static void copyResourceToOutput(String resource, File outputDirectory) {
    InputStream is = null;
    OutputStream os = null;
    try {
      is = ExecutionSummary.class.getResourceAsStream("/" + resource);
      os = new FileOutputStream(new File(outputDirectory, resource));
      IOUtils.copy(is, os);
    } catch (IOException e) {
      throw new RuntimeException("Unable to copy resource " + resource, e);
    } finally {
      try {
        if (is != null) is.close();
      } catch (Exception ignored) {
      }
      try {
        if (os != null) os.close();
      } catch (Exception ignored) {
      }
    }
  }

  public static class Builder {
    private String title;
    private File outputDirectory;
    private long startNano;
    private Calendar started;
    private Calendar ended;
    private Exception exception;
    private List<ExecutionResult> results = synchronizedList(new ArrayList<ExecutionResult>());
    private Map<String, InstrumentationTestClass> testClasses =
      synchronizedMap(new HashMap<String, InstrumentationTestClass>());

    public Builder() {
    }

    public Builder setTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder setOutputDirectory(File outputDirectory) {
      this.outputDirectory = outputDirectory;
      return this;
    }

    public Builder start() {
      if (started != null) {
        throw new IllegalStateException("start() may only be called once.");
      }

      startNano = System.nanoTime();
      started = Calendar.getInstance();
      return this;
    }

    public Builder addResult(ExecutionResult result) {
      results.add(result);
      for (InstrumentationTestClass testClass : result.testClasses()) {
        if (testClasses.containsKey(testClass.className)) {
          // We need to add all our tests to this class.
          for (InstrumentationTest test : testClass.tests()) {
            InstrumentationTestClass existingTestClass = testClasses.get(testClass.className);
            if (existingTestClass.containsTest(test.identifier)) {
              // Merge both tests' results.
              existingTestClass.getTest(test.identifier).mergeResults(test);
            } else {
              existingTestClass.addTest(test);
            }
          }
        } else {
          testClasses.put(testClass.className, testClass);
        }
      }
      return this;
    }

    public Builder setException(Exception exception) {
      if (this.exception != null) {
        throw new IllegalStateException("Only one top-level exception can be set.");
      }
      this.exception = exception;
      return this;
    }

    public ExecutionSummary end() {
      if (started == null) {
        throw new IllegalStateException("You must call start() first.");
      }
      if (ended != null) {
        throw new IllegalStateException("end() may only be called once.");
      }

      ended = Calendar.getInstance();

      int totalTests = 0;
      int totalSuccess = 0;
      int totalFailure = 0;
      for (ExecutionResult result : results) {
        totalTests += result.testsStarted;
        totalSuccess += result.testsStarted - result.testsFailed;
        totalFailure += result.testsFailed;
      }

      long totalTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNano);
      String displayTime = DISPLAY_TIME.get().format(ended.getTime());

      return new ExecutionSummary(outputDirectory, title, results, testClasses, exception,
          totalTime, started, ended, totalTests, totalSuccess, totalFailure, displayTime);
    }
  }
}
