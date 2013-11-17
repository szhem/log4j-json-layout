## Log4J Layout to Format Logs According to the Logstash Json Format

1. [What is it?](#what-is-it)
2. [How to use?](#how-to-use)
3. [Notes](#notes)
4. [License](#license)

### What is it?
Log4J Layout to format logs according to the Logstash json format.

This layout does not have any external dependencies on 3rd party libraries, so it can be easily used within different
environments, for example OSGi runtimes.

Currently the following features are available:

* [Selecting what to log](#selecting-what-to-log)
* [Adding tags and fields](#adding-tags-and-fields)
* [Logging source path](#logging-source-path)

### How to use?

#### Selecting what to log

It is possible to select what fields should be included or excluded from the logs.

By default the following fields are logged:

    {
        "level": "ERROR",
        "logger": "root",
        "message": "Hello World!",
        "mdc": {
            "mdc_key_2": "mdc_val_2",
            "mdc_key_1": "mdc_val_1"
        },
        "ndc": "ndc_1 ndc_2 ndc_3",
        "source_host": "szhem-nb",
        "@timestamp": "2013-11-17T10:21:41.863Z",
        "thread": "main",
        "@version": "1"
    }

If there is an exception the logged message will look like the following one:

    {
        "exception": {
            "message": "Test Exception",
            "class": "java.lang.RuntimeException",
            "stacktrace": "java.lang.RuntimeException: Test Exception\n\tat com.github.szhem.logstash.log4j.LogStashJsonLayoutTest.testSourcePath(LogStashJsonLayoutTest.java:193)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:601)\n\tat org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:47)\n\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)\n\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:44)\n\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)\n\tat org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)\n\tat org.junit.rules.TestWatcher$1.evaluate(TestWatcher.java:55)\n\tat org.junit.rules.RunRules.evaluate(RunRules.java:20)\n\tat org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:271)\n\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:70)\n\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:50)\n\tat org.junit.runners.ParentRunner$3.run(ParentRunner.java:238)\n\tat org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:63)\n\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:236)\n\tat org.junit.runners.ParentRunner.access$000(ParentRunner.java:53)\n\tat org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:229)\n\tat org.junit.runners.ParentRunner.run(ParentRunner.java:309)\n\tat org.junit.runner.JUnitCore.run(JUnitCore.java:160)\n\tat com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:76)\n\tat com.intellij.rt.execution.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:195)\n\tat com.intellij.rt.execution.junit.JUnitStarter.main(JUnitStarter.java:63)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:601)\n\tat com.intellij.rt.execution.application.AppMain.main(AppMain.java:120)"
        },
        "level": "ERROR",
        "logger": "root",
        "message": "Hello World!",
        "source_host": "szhem-nb",
        "@timestamp": "2013-11-17T10:21:41.863Z",
        "thread": "main",
        "@version": "1"
    }

By default `location` is not logged as it's pretty expensive to resolve, but if you need to log it use `includedFields`
property to specify the required fields to be included into the message

    log4j.rootLogger = INFO, stdout

    log4j.appender.stdout=org.apache.log4j.ConsoleAppender
    log4j.appender.stdout.layout=com.github.szhem.logstash.log4j.LogStashJsonLayout
    log4j.appender.stdout.layout.includedFields=location

After that the location will be available in the message

    {
        "level": "ERROR",
        "location": {
            "class": "com.github.szhem.logstash.log4j.LogStashJsonLayoutTest",
            "file": "LogStashJsonLayoutTest.java",
            "method": "testSourcePath",
            "line": "195"
        },
        "logger": "root",
        "message": "Hello World!",
        "source_host": "szhem-nb",
        "@timestamp": "2013-11-17T10:21:41.863Z",
        "thread": "main",
        "@version": "1"
    }

Included and excluded fields can be combined together

    log4j.rootLogger = INFO, stdout

    log4j.appender.stdout=org.apache.log4j.ConsoleAppender
    log4j.appender.stdout.layout=com.github.szhem.logstash.log4j.LogStashJsonLayout
    log4j.appender.stdout.layout.includedFields=location
    log4j.appender.stdout.layout.excludedFields=exception,mdc,ndc


#### Adding tags and fields

Additional fields and tags which must be included into the logged message can be specified like this:

    log4j.rootLogger = INFO, stdout

    log4j.appender.stdout=org.apache.log4j.ConsoleAppender
    log4j.appender.stdout.layout=com.github.szhem.logstash.log4j.LogStashJsonLayout
    log4j.appender.stdout.layout.tags=spring,logstash
    log4j.appender.stdout.layout.fields=type:log4j,format:json

#### Logging source path

If the layout is configured with an instance of `FileAppender` or any of its subclasses then the path of the file the
log messages are sent to will also be included into the message:

    log4j.appender.out=org.apache.log4j.RollingFileAppender
    log4j.appender.out.layout=com.github.szhem.logstash.log4j.LogStashJsonLayout
    log4j.appender.out.file="/tmp/logger.log"
    log4j.appender.out.append=true
    log4j.appender.out.maxFileSize=100MB
    log4j.appender.out.maxBackupIndex=10

With such a configuration the message will contain additional `source_path` field

    {
        "level": "ERROR",
        "logger": "root",
        "message": "Hello World!",
        "source_host": "szhem-nb",
        "source_path": "/tmp/logger.log",
        "@timestamp": "2013-11-17T10:21:41.863Z",
        "thread": "main",
        "@version": "1"
    }

### Notes

The `LogStashJsonLayout` is not thread safe and a single instance of this layout must not be used with multiple
appenders. Each appender must be configured with its own layout instance.

### License

The component is distributed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
