@echo off
setlocal
set "GRADLE_VERSION=8.9"
set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "STAYEASY_GRADLE_HOME=%GRADLE_USER_HOME%\stayeasy-bootstrap\gradle-%GRADLE_VERSION%"
if not exist "%STAYEASY_GRADLE_HOME%\bin\gradle.bat" (
  echo Downloading Gradle %GRADLE_VERSION% for StayEasy (first run only)...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $version='%GRADLE_VERSION%'; $base=Join-Path $env:USERPROFILE '.gradle\stayeasy-bootstrap'; New-Item -ItemType Directory -Force -Path $base | Out-Null; $zip=Join-Path $env:TEMP ('stayeasy-gradle-' + $version + '.zip'); Invoke-WebRequest -Uri ('https://services.gradle.org/distributions/gradle-' + $version + '-bin.zip') -OutFile $zip; Expand-Archive -LiteralPath $zip -DestinationPath $base -Force; Remove-Item -LiteralPath $zip -Force"
  if errorlevel 1 (
    echo Gradle download failed. Check your internet connection and retry.
    exit /b 1
  )
)
"%STAYEASY_GRADLE_HOME%\bin\gradle.bat" -p "%~dp0" %*
exit /b %ERRORLEVEL%
