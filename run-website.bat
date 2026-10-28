@echo off
setlocal
cd /d "%~dp0"
echo Compiling Daily Expenses Tracker website...
javac Main.java Expense.java ExpenseManager.java FileStorage.java WebMain.java
if errorlevel 1 (
  echo.
  echo Compilation failed. Copy the error above and send it for troubleshooting.
  pause
  exit /b 1
)
echo.
echo Starting the website. Keep this window open while using it.
java WebMain
if errorlevel 1 (
  echo.
  echo The website stopped. Check the error above.
  pause
)
endlocal
