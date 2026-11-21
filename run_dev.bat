@echo off
cd /d "%~dp0"

:: Inicia el servidor Rust y el Editor de PC sin dejar ventanas de CMD abiertas
if exist "%~dp0pc-companion-rust\target\release\pc-companion-rust.exe" (
    start "" /B "%~dp0pc-companion-rust\target\release\pc-companion-rust.exe"
) else if exist "%~dp0pc-companion-rust\target\debug\pc-companion-rust.exe" (
    start "" /B "%~dp0pc-companion-rust\target\debug\pc-companion-rust.exe"
)

if exist "%~dp0dist\StreamDockPro-PC\StreamDockWinUI.exe" (
    start "" "%~dp0dist\StreamDockPro-PC\StreamDockWinUI.exe"
) else if exist "%~dp0pc-companion-winui\StreamDockWinUI\bin\Debug\net8.0-windows\StreamDockWinUI.exe" (
    start "" "%~dp0pc-companion-winui\StreamDockWinUI\bin\Debug\net8.0-windows\StreamDockWinUI.exe"
)
exit
