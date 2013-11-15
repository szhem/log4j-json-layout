/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.szhem.logstash.log4j;

import com.github.szhem.logstash.log4j.LogStashJsonLayout;
import com.jayway.jsonassert.JsonAsserter;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.io.StringWriter;
import java.net.InetAddress;

import static com.jayway.jsonassert.JsonAssert.with;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

public class LogStashJsonLayoutTest {

    @Rule
    public TestName testName = new TestName();

    private StringWriter consoleWriter;
    private LogStashJsonLayout consoleLayout;
    private Logger logger;

    @Before
    public void setUp() throws Exception {
        consoleWriter = new StringWriter();

        consoleLayout = new LogStashJsonLayout();
        consoleLayout.activateOptions();

        ConsoleAppender consoleAppender = spy(new ConsoleAppender());
        doNothing().when(consoleAppender).activateOptions();
        consoleAppender.setWriter(consoleWriter);
        consoleAppender.setLayout(consoleLayout);
        consoleAppender.activateOptions();

        logger = Logger.getRootLogger();
        logger.addAppender(consoleAppender);
        logger.setLevel(Level.INFO);
    }

    @Test
    public void testDefaultFields() throws Exception {
        NDC.push("ndc_1");
        NDC.push("ndc_2");
        NDC.push("ndc_3");
        MDC.put("mdc_key_1", "mdc_val_1");
        MDC.put("mdc_key_2", "mdc_val_2");

        @SuppressWarnings("ThrowableInstanceNeverThrown")
        RuntimeException exception = new RuntimeException("Hello World Exception");

        logger.error("Hello World", exception);

        JsonAsserter asserter = with(consoleWriter.toString())
            .assertThat("$.exception.message", equalTo(exception.getMessage()))
            .assertThat("$.exception.class", equalTo(exception.getClass().getName()));
        for (StackTraceElement e : exception.getStackTrace()) {
            asserter
                .assertThat("$.exception.stacktrace", containsString(e.getClassName()))
                .assertThat("$.exception.stacktrace", containsString(e.getMethodName()));
        }
        asserter
            .assertThat("$.level", equalTo("ERROR"))
            .assertThat("$.location", nullValue())
            .assertThat("$.logger", equalTo(logger.getName()))
            .assertThat("$.mdc.mdc_key_1", equalTo("mdc_val_1"))
            .assertThat("$.mdc.mdc_key_2", equalTo("mdc_val_2"))
            .assertThat("$.message", equalTo("Hello World"))
            .assertThat("$.ndc", equalTo("ndc_1 ndc_2 ndc_3"))
            .assertThat("$.source_path", nullValue())
            .assertThat("$.source_host", equalTo(InetAddress.getLocalHost().getHostName()))
            .assertThat("$.tags", nullValue())
            .assertThat("$.thread", equalTo(Thread.currentThread().getName()))
            .assertThat("$.@timestamp", notNullValue())
            .assertThat("$.@version", equalTo("1"));
    }

    @Test
    public void testIncludeFields() throws Exception {
        consoleLayout.setIncludedFields("location");
        consoleLayout.activateOptions();

        logger.info("Hello World");

        with(consoleWriter.toString())
            .assertThat("$.location.class", equalTo(getClass().getName()))
            .assertThat("$.location.file", equalTo(getClass().getSimpleName() + ".java"))
            .assertThat("$.location.method", equalTo(testName.getMethodName()))
            .assertThat("$.location.line", notNullValue());
    }

    @Test
    public void testExcludeFields() throws Exception {
        consoleLayout.setExcludedFields("ndc,mdc,exception");
        consoleLayout.activateOptions();

        NDC.push("ndc_1");
        NDC.push("ndc_2");
        NDC.push("ndc_3");

        MDC.put("mdc_key_1", "mdc_val_1");
        MDC.put("mdc_key_2", "mdc_val_2");

        @SuppressWarnings("ThrowableInstanceNeverThrown")
        RuntimeException exception = new RuntimeException("Hello World Exception");

        logger.error("Hello World", exception);

        with(consoleWriter.toString())
            .assertThat("$.exception", nullValue())
            .assertThat("$.level", equalTo("ERROR"))
            .assertThat("$.logger", equalTo(logger.getName()))
            .assertThat("$.mdc", nullValue())
            .assertThat("$.message", equalTo("Hello World"))
            .assertThat("$.ndc", nullValue())
            .assertThat("$.source_path", nullValue())
            .assertThat("$.source_host", equalTo(InetAddress.getLocalHost().getHostName()))
            .assertThat("$.tags", nullValue())
            .assertThat("$.thread", equalTo(Thread.currentThread().getName()))
            .assertThat("$.@timestamp", notNullValue())
            .assertThat("$.@version", equalTo("1"));
    }

    @Test
    public void testAddTags() throws Exception {
        consoleLayout.setTags("json,logstash");
        consoleLayout.activateOptions();

        logger.info("Hello World");

        with(consoleWriter.toString()).assertThat("$.tags", hasItems("json", "logstash"));
    }

    @Test
    public void testAddFields() throws Exception {
        consoleLayout.setFields("type:log4j,shipper:logstash");
        consoleLayout.activateOptions();

        logger.info("Hello World");

        with(consoleWriter.toString())
            .assertThat("$.type", equalTo("log4j"))
            .assertThat("$.shipper", equalTo("logstash"));
    }

    @Test
    public void testSourcePath() throws Exception {
        logger.info("Hello World!");
        with(consoleWriter.toString()).assertThat("$.source_path", nullValue());

        // for the file appender there must be log file path in the json
        StringWriter fileWriter = new StringWriter();

        LogStashJsonLayout fileLayout = new LogStashJsonLayout();
        fileLayout.activateOptions();

        FileAppender fileAppender = spy(new FileAppender());
        doNothing().when(fileAppender).activateOptions();
        fileAppender.setWriter(fileWriter);
        fileAppender.setFile("/tmp/logger.log");
        fileAppender.setLayout(fileLayout);
        fileAppender.activateOptions();

        logger.addAppender(fileAppender);

        logger.info("Hello World!");
        with(fileWriter.toString())
            .assertThat("$.source_path", equalTo(new File(fileAppender.getFile()).getCanonicalPath()));
    }

    @Test
    public void testEscape() throws Exception {
        logger.info("H\"e\\l/\nl\ro\u0000W\bo\tr\fl\u0001d");

        with(consoleWriter.toString())
            .assertThat("$.message", equalTo("H\"e\\l/\nl\ro\u0000W\bo\tr\fl\u0001d"));
    }
}
