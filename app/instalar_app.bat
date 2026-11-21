@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo ========================================================
echo      STREAM DOCK PRO - INSTALADOR PARA ANDROID
echo ========================================================
echo.

cd /d "%~dp0"

:: 1. Localizar ADB
set "ADB_CMD="
where adb >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set "ADB_CMD=adb"
) else if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set "ADB_CMD=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
) else if defined ANDROID_HOME if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    set "ADB_CMD=%ANDROID_HOME%\platform-tools\adb.exe"
)

if "%ADB_CMD%"=="" (
    echo [ERROR] No se encontro adb.exe en el sistema ni en %LOCALAPPDATA%\Android\Sdk\platform-tools
    pause
    exit /b 1
)

echo [1/4] Verificando dispositivo Android conectado...
"%ADB_CMD%" start-server >nul 2>nul
"%ADB_CMD%" devices
echo.

"%ADB_CMD%" get-state >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] No se detecto ningun dispositivo Android conectado o autorizado.
    echo         - Conecta el telefono por USB.
    echo         - Activa la "Depuracion por USB" en Opciones de Desarrollador.
    echo         - Acepta el permiso de depuracion en la pantalla del telefono.
    echo.
    pause
    exit /b 1
)

:: 2. Compilar APK Debug
set "APK_PATH=%~dp0app\build\outputs\apk\debug\app-debug.apk"
echo [2/4] Compilando APK actualizada (assembleDebug)...
if exist "%~dp0gradlew.bat" (
    call "%~dp0gradlew.bat" :app:assembleDebug
    if !ERRORLEVEL! NEQ 0 (
        echo.
        echo [ERROR] Fallo la compilacion con Gradle.
        pause
        exit /b 1
    )
) else if not exist "%APK_PATH%" (
    echo [ERROR] No se encontro gradlew.bat ni un APK compilado en:
    echo         %APK_PATH%
    pause
    exit /b 1
)

if not exist "%APK_PATH%" (
    echo [ERROR] No se encontro el archivo APK en:
    echo         %APK_PATH%
    pause
    exit /b 1
)

:: 3. Instalar APK y configurar tunel USB
echo.
echo [3/4] Instalando Stream Dock Pro en el dispositivo...
"%ADB_CMD%" install -r -t "%APK_PATH%"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] No se pudo instalar el APK en el dispositivo.
    pause
    exit /b 1
)

echo.
echo [4/4] Configurando tunel USB (adb reverse tcp:8081 tcp:8081) y abriendo la App...
"%ADB_CMD%" reverse tcp:8081 tcp:8081
"%ADB_CMD%" shell am start -n com.streamdock.pro/.MainActivity

echo.
echo ========================================================
echo  [OK] Stream Dock Pro instalado y ejecutandose!
echo ========================================================
pause
