#!/bin/sh
# Standard Gradle wrapper launcher script.
# If gradle/wrapper/gradle-wrapper.jar is missing, open this project in
# Android Studio first and let it sync — it will regenerate the wrapper.
DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$DIR/gradlew.bat" "$@" 2>/dev/null || java -jar "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
