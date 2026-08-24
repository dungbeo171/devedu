@REM Maven Wrapper startup script for Windows
@ECHO OFF
SETLOCAL
SET "MAVEN_PROJECTBASEDIR=%~dp0."
SET "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Missing %WRAPPER_JAR%
  EXIT /B 1
)
IF NOT "%JAVA_HOME%"=="" (
  SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) ELSE (
  SET "JAVA_EXE=java"
)
"%JAVA_EXE%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
SET "MAVEN_EXIT_CODE=%ERRORLEVEL%"
ENDLOCAL & EXIT /B %MAVEN_EXIT_CODE%
