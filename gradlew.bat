@echo off
set APP_HOME=%~dp0
if defined JAVA_HOME (set JAVA_EXE=%JAVA_HOME%/bin/java.exe) else (set JAVA_EXE=java.exe)
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
"%JAVA_EXE%" -Xmx64m -Xms64m -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
