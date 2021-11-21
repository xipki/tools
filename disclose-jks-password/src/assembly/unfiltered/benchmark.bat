@ECHO OFF

SET DIR=%CD%

SET MAIN_CLASS=org.xipki.jksfail.FindPasswordBenchmark

if "x%JAVA_HOME%" == "x" (
  SET JAVA_EXEC=java
) else (
  SET JAVA_EXEC="%JAVA_HOME%\bin\java"
)

%JAVA_EXEC% -cp %DIR%\lib;%DIR%\lib\* %MAIN_CLASS% %*
