SET DIR=%CD%

REM Example: run.bat -d examples\password-dict.txt -k examples\keystore-rsa.jks

SET MAIN_CLASS=org.xipki.jksfail.Main

if "x%JAVA_HOME%" == "x" (
  SET JAVA_EXEC=java
  )
else (
  SET JAVA_EXEC="%JAVA_HOME%\bin\java"
  )

%JAVA_EXEC% -cp %DIR%\lib;%DIR%\lib\* %MAIN_CLASS% %*
