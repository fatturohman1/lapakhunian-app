#!/usr/bin/env sh

##############################################################################
##  Gradle start up script for UN*X (uses JAVA_HOME when set)
##############################################################################

APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ]; then
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
        exit 1
    fi
else
    JAVACMD=java
    command -v java >/dev/null 2>&1 || { echo "ERROR: java not found, set JAVA_HOME"; exit 1; }
fi

exec "$JAVACMD" $JAVA_OPTS $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
