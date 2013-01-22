Spoon
=====

Command line utility which aids in the deployment, execution, and aggregation of
instrumentation tests across multiple devices.



Introduction
------------

Android's ever-expanding ecosystem of devices creates a unique challenge to
testing applications. Spoon aims to simplify this task by distributing
instrumentation test execution and displaying the results in a meaningful way.

Instead of attempting to be a new form of testing, Spoon makes existing
instrumentation tests more useful. Using the application APK and instrumentation
APK Spoon runs the tests on all devices which are visible to ADB. Once all tests
have completed a static HTML summary is generated with detailed information
about each device and test.



Screenshots
-----------

In addition to simply running instrumentation tests, Spoon has the ability to
snap screenshots at key points during your tests which are then included in the
output. This allows for visual inspection of test executions across different
devices.

Taking screenshots requires that you include the `spoon` JAR in your
instrumentation app. In your tests call the `screenshot` method with a
human-readable tag.

```java
Spoon.screenshot(activity, "initial_state");
/* Normal test code... */
Spoon.screenshot(activity, "after_login");
```

The tag specified will be used to identify and compare screenshots taken across
multiple test runs.

You can also view each test's screenshots as an animated GIF to gauge the actual
sequence of interaction.



Download
--------

Download [the latest runner JAR][1] or [the latest screenshot JAR][2], or grab
via Maven:

```xml
<dependency>
  <groupId>com.squareup.spoon</groupId>
  <artifactId>spoon</artifactId>
  <version>(insert latest version)</version>
</dependency>
```



Execution
---------

Spoon was designed to be run both as a standalone tool or directly as part of
your build system.

You can run Spoon as a standalone tool with your application and instrumentation
APKs.

```
java -jar spoon-runner-1.0.0-jar-with-dependencies.jar \
    --apk example-app.apk \
    --test-apk example-tests.apk
```

By default the output will be placed in a spoon-output/ folder of the current
directory. You can control additional parameters of the execution using other
flags.

```
Options:
    --apk               Application APK
    --fail-on-failure   Non-zero exit code on failure
    --output            Output path
    --sdk               Path to Android SDK
    --test-apk          Test application APK
    --title             Execution title
```

If you are using Maven for compilation, a plugin is provided for easy execution.
Declare the plugin in the `pom.xml` for the instrumentation test module.

```xml
<plugin>
  <groupId>com.squareup.spoon</groupId>
  <artifactId>spoon-maven-plugin</artifactId>
  <version>(insert latest version)</version>
</plugin>
```

The plugin will look for an `apk` dependency for the corresponding application.
Typically this is specified in parallel with the `jar` dependency on the
application.

```xml
<dependency>
  <groupId>com.example</groupId>
  <artifactId>example-app</artifactId>
  <version>${project.version}</version>
  <type>jar</type>
  <scope>provied</scope>
</dependency>
<dependency>
  <groupId>com.example</groupId>
  <artifactId>example-app</artifactId>
  <version>${project.version}</version>
  <type>apk</type>
  <scope>provied</scope>
</dependency>
```

You can invoke the plugin by running `mvn spoon:run`. The execution result will
be placed in the `target/spoon-output/` folder.

For a working example see the sample application and instrumentation tests in
the `spoon-sample/` folder.



License
--------

    Copyright 2013 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.




 [1]: http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.squareup.spoon&a=spoon-runner&v=LATEST
 [2]: http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.squareup.spoon&a=spoon&v=LATEST