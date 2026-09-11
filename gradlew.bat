@echo off
setlocal
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
echo Questo progetto e' ottimizzato per AndroidIDE/Linux. Installa Gradle 8.9 oppure usa Android Studio con Gradle wrapper.
exit /b 1
