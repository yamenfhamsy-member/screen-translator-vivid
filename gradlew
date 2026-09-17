#!/bin/sh
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd -P)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_PROPERTIES="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"
if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Could not find $WRAPPER_JAR" >&2
  exit 1
fi
if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi
exec "$JAVACMD" -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
