@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion
title Generador de APK - Stream Dock Pro Android

echo ========================================================
echo      STREAM DOCK PRO - GENERADOR DE APK ANDROID
echo ========================================================
echo.

cd /d "%~dp0"
set "ROOT_DIR=%~dp0"
set "APP_DIR=%ROOT_DIR%app"
set "OUT_DIR=%ROOT_DIR%dist\Android"

:: 1. Verificar o detectar JAVA_HOME (JBR de Android Studio)
if not defined JAVA_HOME (
    if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
        set "PATH=!JAVA_HOME!\bin;!PATH!"
    ) else if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
        set "PATH=!JAVA_HOME!\bin;!PATH!"
    )
)

echo [1/3] Compilando APK firmado (Release y Debug) con Gradle...
pushd "%APP_DIR%"
call "%APP_DIR%\gradlew.bat" :app:assembleRelease :app:assembleDebug
if errorlevel 1 (
    popd
    echo.
    echo [ERROR] Fallo la compilacion del APK.
    pause
    exit /b 1
)
popd

echo.
echo [2/3] Exportando APK listo para instalar...
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

set "RELEASE_APK=%APP_DIR%\app\build\outputs\apk\release\app-release.apk"
set "DEBUG_APK=%APP_DIR%\app\build\outputs\apk\debug\app-debug.apk"

if exist "%RELEASE_APK%" (
    copy /Y "%RELEASE_APK%" "%OUT_DIR%\StreamDockPro.apk" >nul
    echo   [OK] APK Release firmado : "%OUT_DIR%\StreamDockPro.apk"
)

if exist "%DEBUG_APK%" (
    copy /Y "%DEBUG_APK%" "%OUT_DIR%\StreamDockPro-debug.apk" >nul
    if not exist "%OUT_DIR%\StreamDockPro.apk" (
        copy /Y "%DEBUG_APK%" "%OUT_DIR%\StreamDockPro.apk" >nul
    )
    echo   [OK] APK Debug           : "%OUT_DIR%\StreamDockPro-debug.apk"
)

:: Copiar tambien al Escritorio para acceso rapido
for /f "usebackq delims=" %%D in (`powershell -NoProfile -Command "[Environment]::GetFolderPath('Desktop')"`) do set "DESKTOP_DIR=%%D"
if exist "%OUT_DIR%\StreamDockPro.apk" (
    copy /Y "%OUT_DIR%\StreamDockPro.apk" "!DESKTOP_DIR!\StreamDockPro.apk" >nul
    echo   [OK] Copia en Escritorio : "!DESKTOP_DIR!\StreamDockPro.apk"
)

echo.
echo [3/3] Verificando si hay un telefono Android conectado por USB...
set "ADB_EXE="
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set "ADB_EXE=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
) else (
    where adb >nul 2>&1
    if not errorlevel 1 set "ADB_EXE=adb"
)

if defined ADB_EXE (
    for /f "skip=1 tokens=1,2" %%A in ('"%ADB_EXE%" devices') do (
        if "%%B"=="device" (
            echo   - Dispositivo detectado ^(%%A^). Instalando StreamDockPro.apk...
            "%ADB_EXE%" -s %%A reverse tcp:8081 tcp:8081 >nul 2>&1
            "%ADB_EXE%" -s %%A install -r -t "%OUT_DIR%\StreamDockPro.apk"
            "%ADB_EXE%" -s %%A shell am start -n com.streamdock.pro/.MainActivity >nul 2>&1
        )
    )
)

echo.
echo ========================================================
echo  [OK] APK CREADO EXITOSAMENTE
echo ========================================================
echo  - Carpeta de salida : "%OUT_DIR%\StreamDockPro.apk"
echo  - Copia Escritorio  : "!DESKTOP_DIR!\StreamDockPro.apk"
echo ========================================================
echo.

explorer "%OUT_DIR%"
pause
