package com.squareup.spoon;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;

import static java.util.Collections.synchronizedList;

public class ExecutionSummary {
  static final ThreadLocal<DateFormat> DISPLAY_TIME = new ThreadLocal<DateFormat>() {
    @Override protected DateFormat initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd hh:mm a");
    }
  };

  public final List<ExecutionResult> results = synchronizedList(new ArrayList<ExecutionResult>());
  public final List<Exception> exceptions = synchronizedList(new ArrayList<Exception>());
  private final File output;
  public final String title;
  public long testStart;
  public long testEnd;
  public Date testCompleted;
  public int totalTests;
  public int totalSuccess;
  public int totalFailure;
  public int totalExceptions;
  public long totalTime;
  public String displayTime;

  public ExecutionSummary(String title, File output) {
    this.title = title;
    this.output = output;
  }

  public void updateDynamicValues() {
    totalTests = 0;
    totalSuccess = 0;
    totalFailure = 0;
    for (ExecutionResult result : results) {
      totalTests += result.testsStarted;
      totalSuccess += result.testsStarted - result.testsFailed;
      totalFailure += result.testsFailed;
    }

    totalExceptions = exceptions.size();
    totalTime = TimeUnit.NANOSECONDS.toSeconds(testEnd - testStart);
    displayTime = DISPLAY_TIME.get().format(testCompleted);
  }

  public void generateHtml() {
    updateDynamicValues();

    copyResourceToOutput("bootstrap.min.css", output);
    copyResourceToOutput("bootstrap.min.js", output);
    copyResourceToOutput("jquery.min.js", output);
    copyResourceToOutput("spoon.css", output);

    DefaultMustacheFactory mustacheFactory = new DefaultMustacheFactory();
    Mustache summary = mustacheFactory.compile("index.html");
    Mustache device = mustacheFactory.compile("index-device.html");
    try {
      summary.execute(new FileWriter(new File(output, "index.html")), this).flush();

      for (ExecutionResult result : results) {
        result.updateDynamicValues();

        File deviceOutput = new File(output, result.serial + "/index.html");
        device.execute(new FileWriter(deviceOutput), result).flush();
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
}
