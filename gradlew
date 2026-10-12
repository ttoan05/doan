#!/bin/sh
APP_HOME='.'
APP_BASE_NAME=$(basename "$0")
PRG="$0"
while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG"); link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' > /dev/null; then PRG="$link"
  else PRG=$(dirname "$PRG")"/$link"; fi
done
cd "$(dirname "$PRG")/" >/dev/null
APP_HOME="$(pwd -P)"
if [ -n "$JAVA_HOME" ]; then JAVACMD="$JAVA_HOME/bin/java"; else JAVACMD="java"; fi
exec "$JAVACMD" -Xmx64m -Xms64m \
  "-Dorg.gradle.appname=$APP_BASE_NAME" \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
