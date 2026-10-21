@echo off
rem Double-click this file to build the mod. No typing needed.
cd /d "%~dp0"
echo Building the Mutations mod... the first build can take 5-15 minutes.
echo.
call gradlew.bat build
echo.
if exist "build\libs\mutations-0.1.0.jar" (
    echo DONE! Your mod is here:
    echo %~dp0build\libs\mutations-0.1.0.jar
    explorer "%~dp0build\libs"
) else (
    echo The build did not finish. Scroll up, find the first lines that say "error",
    echo and send a screenshot of them to Claude.
)
echo.
pause
