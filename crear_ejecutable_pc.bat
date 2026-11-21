@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion
title Generador de Instalador Unico (.EXE) - Stream Dock Pro PC

echo ========================================================
echo    STREAM DOCK PRO - GENERADOR DE EJECUTABLE UNICO
echo    (Servidor Rust + Editor PC en 1 solo .EXE sin CMD)
echo ========================================================
echo.

cd /d "%~dp0"
set "ROOT_DIR=%~dp0"
set "BUILD_TMP=%ROOT_DIR%dist\tmp_publish"
set "DIST_DIR=%ROOT_DIR%dist"
set "ICON_PATH=%ROOT_DIR%pc-companion-winui\StreamDockWinUI\app_icon.ico"
set "CSC_EXE=%SystemRoot%\Microsoft.NET\Framework64\v4.0.30319\csc.exe"

echo [1/5] Cerrando instancias previas para liberar archivos...
taskkill /F /IM StreamDockWinUI.exe >nul 2>&1
taskkill /F /IM StreamDockPro.exe >nul 2>&1
taskkill /F /IM pc-companion-rust.exe >nul 2>&1
timeout /t 1 /nobreak >nul

if exist "%BUILD_TMP%" rmdir /S /Q "%BUILD_TMP%"
mkdir "%BUILD_TMP%"
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"

echo [2/5] Generando icono oficial de alta resolucion (256x256 multi-capa .ICO)...
set "ICON_GEN_CS=%BUILD_TMP%\IconGen.cs"
(
echo using System;
echo using System.Collections.Generic;
echo using System.Drawing;
echo using System.Drawing.Drawing2D;
echo using System.Drawing.Imaging;
echo using System.IO;
echo.
echo public class IconGen
echo {
echo     public static void Main^(string[] args^)
echo     {
echo         string outPath = args[0];
echo         int[] sizes = new int[] { 256, 64, 48, 32, 16 };
echo         List^<byte[]^> pngFrames = new List^<byte[]^>^(^);
echo         foreach ^(int sz in sizes^)
echo         {
echo             using ^(Bitmap bmp = RenderDeckIcon^(sz^)^)
echo             using ^(MemoryStream ms = new MemoryStream^(^)^)
echo             {
echo                 bmp.Save^(ms, ImageFormat.Png^);
echo                 pngFrames.Add^(ms.ToArray^(^)^);
echo             }
echo         }
echo         using ^(FileStream fs = new FileStream^(outPath, FileMode.Create, FileAccess.Write^)^)
echo         using ^(BinaryWriter bw = new BinaryWriter^(fs^)^)
echo         {
echo             bw.Write^((ushort^)0^);
echo             bw.Write^((ushort^)1^);
echo             bw.Write^((ushort^)sizes.Length^);
echo             int offset = 6 + ^(16 * sizes.Length^);
echo             for ^(int i = 0; i ^< sizes.Length; i++^)
echo             {
echo                 int sz = sizes[i];
echo                 byte bSize = ^(sz ^>= 256^) ? ^(byte^)0 : ^(byte^)sz;
echo                 bw.Write^(bSize^);
echo                 bw.Write^(bSize^);
echo                 bw.Write^((byte^)0^);
echo                 bw.Write^((byte^)0^);
echo                 bw.Write^((ushort^)1^);
echo                 bw.Write^((ushort^)32^);
echo                 bw.Write^(pngFrames[i].Length^);
echo                 bw.Write^(offset^);
echo                 offset += pngFrames[i].Length;
echo             }
echo             for ^(int i = 0; i ^< sizes.Length; i++^)
echo             {
echo                 bw.Write^(pngFrames[i]^);
echo             }
echo         }
echo     }
echo.
echo     private static Bitmap RenderDeckIcon^(int size^)
echo     {
echo         Bitmap bmp = new Bitmap^(size, size, PixelFormat.Format32bppArgb^);
echo         using ^(Graphics g = Graphics.FromImage^(bmp^)^)
echo         {
echo             g.SmoothingMode = SmoothingMode.AntiAlias;
echo             g.InterpolationMode = InterpolationMode.HighQualityBicubic;
echo             g.PixelOffsetMode = PixelOffsetMode.HighQuality;
echo             g.Clear^(Color.Transparent^);
echo.
echo             float pad = size * 0.05f;
echo             float boxSize = size - ^(pad * 2f^);
echo             float outerRadius = size * 0.22f;
echo.
echo             using ^(GraphicsPath outerPath = RoundedRect^(pad, pad, boxSize, boxSize, outerRadius^)^)
echo             {
echo                 using ^(LinearGradientBrush bgBrush = new LinearGradientBrush^(
echo                     new RectangleF^(0, 0, size, size^),
echo                     Color.FromArgb^(255, 24, 24, 38^),
echo                     Color.FromArgb^(255, 13, 13, 22^),
echo                     45f^)^)
echo                 {
echo                     g.FillPath^(bgBrush, outerPath^);
echo                 }
echo                 using ^(Pen borderPen = new Pen^(Color.FromArgb^(220, 137, 180, 250^), Math.Max^(1.5f, size * 0.032f^)^)^)
echo                 {
echo                     g.DrawPath^(borderPen, outerPath^);
echo                 }
echo             }
echo.
echo             Color[,] tileColors = new Color[3, 3]
echo             {
echo                 { Color.FromArgb^(255, 79, 70, 229^),  Color.FromArgb^(255, 243, 139, 168^), Color.FromArgb^(255, 29, 185, 84^) },
echo                 { Color.FromArgb^(255, 52, 152, 219^), Color.FromArgb^(255, 155, 89, 182^),  Color.FromArgb^(255, 250, 179, 135^) },
echo                 { Color.FromArgb^(255, 46, 204, 113^), Color.FromArgb^(255, 137, 180, 250^), Color.FromArgb^(255, 249, 226, 175^) }
echo             };
echo.
echo             float gridMargin = size * 0.17f;
echo             float gridArea = size - ^(gridMargin * 2f^);
echo             float gap = size * 0.045f;
echo             float tileSize = ^(gridArea - ^(gap * 2f^)^) / 3f;
echo             float tileRadius = Math.Max^(2f, tileSize * 0.26f^);
echo.
echo             for ^(int r = 0; r ^< 3; r++^)
echo             {
echo                 for ^(int c = 0; c ^< 3; c++^)
echo                 {
echo                     float tx = gridMargin + c * ^(tileSize + gap^);
echo                     float ty = gridMargin + r * ^(tileSize + gap^);
echo                     Color baseCol = tileColors[r, c];
echo                     Color lightCol = ControlPaintLight^(baseCol, 0.25f^);
echo.
echo                     using ^(GraphicsPath tp = RoundedRect^(tx, ty, tileSize, tileSize, tileRadius^)^)
echo                     using ^(LinearGradientBrush tb = new LinearGradientBrush^(
echo                         new RectangleF^(tx, ty, tileSize, tileSize^),
echo                         lightCol, baseCol, 55f^)^)
echo                     {
echo                         g.FillPath^(tb, tp^);
echo                         using ^(Pen tpPen = new Pen^(Color.FromArgb^(110, 255, 255, 255^), Math.Max^(1f, size * 0.01f^)^)^)
echo                         {
echo                             g.DrawPath^(tpPen, tp^);
echo                         }
echo                     }
echo.
echo                     if ^(r == 0 ^&^& c == 0 ^&^& size ^>= 32^)
echo                     {
echo                         PointF[] playPts = new PointF[]
echo                         {
echo                             new PointF^(tx + tileSize * 0.36f, ty + tileSize * 0.27f^),
echo                             new PointF^(tx + tileSize * 0.36f, ty + tileSize * 0.73f^),
echo                             new PointF^(tx + tileSize * 0.74f, ty + tileSize * 0.50f^)
echo                         };
echo                         g.FillPolygon^(Brushes.White, playPts^);
echo                     }
echo                     else if ^(r == 0 ^&^& c == 1 ^&^& size ^>= 32^)
echo                     {
echo                         float dotSz = tileSize * 0.42f;
echo                         g.FillEllipse^(Brushes.White, tx + ^(tileSize - dotSz^) / 2f, ty + ^(tileSize - dotSz^) / 2f, dotSz, dotSz^);
echo                     }
echo                 }
echo             }
echo         }
echo         return bmp;
echo     }
echo.
echo     private static Color ControlPaintLight^(Color c, float factor^)
echo     {
echo         int r = Math.Min^(255, c.R + ^(int^)^((255 - c.R^) * factor^)^);
echo         int g = Math.Min^(255, c.G + ^(int^)^((255 - c.G^) * factor^)^);
echo         int b = Math.Min^(255, c.B + ^(int^)^((255 - c.B^) * factor^)^);
echo         return Color.FromArgb^(c.A, r, g, b^);
echo     }
echo.
echo     private static GraphicsPath RoundedRect^(float x, float y, float w, float h, float r^)
echo     {
echo         float d = r * 2f;
echo         GraphicsPath path = new GraphicsPath^(^);
echo         path.AddArc^(x, y, d, d, 180, 90^);
echo         path.AddArc^(x + w - d, y, d, d, 270, 90^);
echo         path.AddArc^(x + w - d, y + h - d, d, d, 0, 90^);
echo         path.AddArc^(x, y + h - d, d, d, 90, 90^);
echo         path.CloseFigure^(^);
echo         return path;
echo     }
echo }
) > "%ICON_GEN_CS%"

"%CSC_EXE%" /nologo /optimize+ /out:"%BUILD_TMP%\IconGen.exe" /r:System.dll /r:System.Drawing.dll "%ICON_GEN_CS%"
"%BUILD_TMP%\IconGen.exe" "%ICON_PATH%"
copy /Y "%ICON_PATH%" "%DIST_DIR%\StreamDockPro.ico" >nul

echo [3/5] Compilando Servidor Rust en modo Release (100%% silencioso sin ventana CMD)...
cargo build --release --manifest-path "%ROOT_DIR%pc-companion-rust\Cargo.toml"
if errorlevel 1 (
    echo [ERROR] Fallo la compilacion del Servidor Rust.
    pause
    exit /b 1
)

echo.
echo [4/5] Empaquetando Servidor Rust + Icono DENTRO del Editor de PC en un solo ejecutable...
dotnet publish "%ROOT_DIR%pc-companion-winui\StreamDockWinUI\StreamDockWinUI.csproj" ^
    -c Release ^
    -r win-x64 ^
    --self-contained false ^
    -p:PublishSingleFile=true ^
    -p:IncludeNativeLibrariesForSelfExtract=true ^
    -p:IncludeAllContentForSelfExtract=true ^
    -p:DebugType=None ^
    -p:DebugSymbols=false ^
    -o "%BUILD_TMP%\app_out"

if errorlevel 1 (
    echo [ERROR] Fallo la publicacion del ejecutable unificado.
    pause
    exit /b 1
)

copy /Y "%BUILD_TMP%\app_out\StreamDockWinUI.exe" "%DIST_DIR%\StreamDockPro.exe" >nul

echo.
echo [5/5] Creando Instalador Grafico con Icono (StreamDockPro_Instalador.exe)...

set "INSTALLER_CS=%BUILD_TMP%\InstallerProgram.cs"
(
echo using System;
echo using System.Diagnostics;
echo using System.Drawing;
echo using System.IO;
echo using System.Reflection;
echo using System.Windows.Forms;
echo.
echo [assembly: AssemblyTitle^("Stream Dock Pro - Instalador Oficial"^)]
echo [assembly: AssemblyProduct^("Stream Dock Pro"^)]
echo [assembly: AssemblyVersion^("1.0.0.0"^)]
echo.
echo namespace StreamDockInstaller
echo {
echo     public class InstallerForm : Form
echo     {
echo         private Button btnInstall;
echo         private Label lblStatus;
echo         private ProgressBar progressBar;
echo         private CheckBox chkDesktop;
echo         private CheckBox chkLaunch;
echo.
echo         public InstallerForm^(^)
echo         {
echo             Text = "Instalador de Stream Dock Pro (Servidor Rust + Editor PC)";
echo             Size = new Size^(480, 310^);
echo             FormBorderStyle = FormBorderStyle.FixedDialog;
echo             MaximizeBox = false;
echo             StartPosition = FormStartPosition.CenterScreen;
echo             BackColor = Color.FromArgb^(17, 17, 27^);
echo             ForeColor = Color.White;
echo             try { Icon = Icon.ExtractAssociatedIcon^(Application.ExecutablePath^); } catch { }
echo.
echo             var lblTitle = new Label
echo             {
echo                 Text = "Stream Dock Pro",
echo                 Font = new Font^("Segoe UI", 18, FontStyle.Bold^),
echo                 ForeColor = Color.FromArgb^(137, 180, 250^),
echo                 Location = new Point^(24, 20^),
echo                 AutoSize = true
echo             };
echo.
echo             var lblDesc = new Label
echo             {
echo                 Text = "Este instalador configura el Servidor Rust silencioso y el Editor de PC\nen un solo paquete sin ventanas de consola (CMD).",
echo                 Font = new Font^("Segoe UI", 9.5f^),
echo                 ForeColor = Color.FromArgb^(166, 173, 200^),
echo                 Location = new Point^(26, 60^),
echo                 Size = new Size^(420, 42^)
echo             };
echo.
echo             chkDesktop = new CheckBox
echo             {
echo                 Text = "Crear acceso directo en el Escritorio y Menu Inicio",
echo                 Checked = true,
echo                 Font = new Font^("Segoe UI", 9.5f^),
echo                 ForeColor = Color.FromArgb^(205, 214, 244^),
echo                 Location = new Point^(28, 112^),
echo                 AutoSize = true
echo             };
echo.
echo             chkLaunch = new CheckBox
echo             {
echo                 Text = "Iniciar Stream Dock Pro al finalizar la instalacion",
echo                 Checked = true,
echo                 Font = new Font^("Segoe UI", 9.5f^),
echo                 ForeColor = Color.FromArgb^(205, 214, 244^),
echo                 Location = new Point^(28, 140^),
echo                 AutoSize = true
echo             };
echo.
echo             progressBar = new ProgressBar
echo             {
echo                 Location = new Point^(28, 175^),
echo                 Size = new Size^(410, 18^),
echo                 Style = ProgressBarStyle.Continuous,
echo                 Value = 0
echo             };
echo.
echo             lblStatus = new Label
echo             {
echo                 Text = "Listo para instalar.",
echo                 Font = new Font^("Segoe UI", 9f^),
echo                 ForeColor = Color.FromArgb^(166, 227, 161^),
echo                 Location = new Point^(28, 198^),
echo                 Size = new Size^(410, 20^)
echo             };
echo.
echo             btnInstall = new Button
echo             {
echo                 Text = "Instalar Stream Dock Pro",
echo                 Font = new Font^("Segoe UI", 10.5f, FontStyle.Bold^),
echo                 BackColor = Color.FromArgb^(137, 180, 250^),
echo                 ForeColor = Color.FromArgb^(17, 17, 27^),
echo                 FlatStyle = FlatStyle.Flat,
echo                 Location = new Point^(28, 224^),
echo                 Size = new Size^(410, 38^),
echo                 Cursor = Cursors.Hand
echo             };
echo             btnInstall.FlatAppearance.BorderSize = 0;
echo             btnInstall.Click += OnInstallClick;
echo.
echo             Controls.Add^(lblTitle^);
echo             Controls.Add^(lblDesc^);
echo             Controls.Add^(chkDesktop^);
echo             Controls.Add^(chkLaunch^);
echo             Controls.Add^(progressBar^);
echo             Controls.Add^(lblStatus^);
echo             Controls.Add^(btnInstall^);
echo         }
echo.
echo         private void OnInstallClick^(object sender, EventArgs e^)
echo         {
echo             try
echo             {
echo                 btnInstall.Enabled = false;
echo                 progressBar.Value = 20;
echo                 lblStatus.Text = "Cerrando procesos previos...";
echo                 Application.DoEvents^(^);
echo.
echo                 foreach ^(var p in Process.GetProcessesByName^("StreamDockWinUI"^)^) { try { p.Kill^(^); p.WaitForExit^(1000^); } catch { } }
echo                 foreach ^(var p in Process.GetProcessesByName^("StreamDockPro"^)^) { try { p.Kill^(^); p.WaitForExit^(1000^); } catch { } }
echo                 foreach ^(var p in Process.GetProcessesByName^("pc-companion-rust"^)^) { try { p.Kill^(^); p.WaitForExit^(1000^); } catch { } }
echo.
echo                 progressBar.Value = 50;
echo                 lblStatus.Text = "Extrayendo Servidor Rust y Editor de PC...";
echo                 Application.DoEvents^(^);
echo.
echo                 string installDir = Path.Combine^(Environment.GetFolderPath^(Environment.SpecialFolder.LocalApplicationData^), "Programs", "StreamDockPro"^);
echo                 Directory.CreateDirectory^(installDir^);
echo                 string targetExe = Path.Combine^(installDir, "StreamDockPro.exe"^);
echo.
echo                 using ^(var stream = Assembly.GetExecutingAssembly^(^).GetManifestResourceStream^("StreamDockProPayload.exe"^)^)
echo                 {
echo                     using ^(var fs = new FileStream^(targetExe, FileMode.Create, FileAccess.Write^)^)
echo                     {
echo                         stream.CopyTo^(fs^);
echo                     }
echo                 }
echo.
echo                 progressBar.Value = 80;
echo                 if ^(chkDesktop.Checked^)
echo                 {
echo                     lblStatus.Text = "Creando accesos directos...";
echo                     Application.DoEvents^(^);
echo                     CreateShortcut^(Path.Combine^(Environment.GetFolderPath^(Environment.SpecialFolder.DesktopDirectory^), "Stream Dock Pro.lnk"^), targetExe, installDir^);
echo                     CreateShortcut^(Path.Combine^(Environment.GetFolderPath^(Environment.SpecialFolder.Programs^), "Stream Dock Pro.lnk"^), targetExe, installDir^);
echo                 }
echo.
echo                 progressBar.Value = 100;
echo                 lblStatus.Text = "Instalacion completada con exito.";
echo                 Application.DoEvents^(^);
echo.
echo                 if ^(chkLaunch.Checked^)
echo                 {
echo                     Process.Start^(new ProcessStartInfo
echo                     {
echo                         FileName = targetExe,
echo                         WorkingDirectory = installDir,
echo                         UseShellExecute = true
echo                     }^);
echo                 }
echo                 Close^(^);
echo             }
echo             catch ^(Exception ex^)
echo             {
echo                 btnInstall.Enabled = true;
echo                 MessageBox.Show^("Error durante la instalacion: " + ex.Message, "Stream Dock Pro", MessageBoxButtons.OK, MessageBoxIcon.Error^);
echo             }
echo         }
echo.
echo         private static void CreateShortcut^(string shortcutPath, string targetExe, string workDir^)
echo         {
echo             try
echo             {
echo                 Type shellType = Type.GetTypeFromProgID^("WScript.Shell"^);
echo                 dynamic shell = Activator.CreateInstance^(shellType^);
echo                 dynamic sc = shell.CreateShortcut^(shortcutPath^);
echo                 sc.TargetPath = targetExe;
echo                 sc.WorkingDirectory = workDir;
echo                 sc.IconLocation = targetExe + ",0";
echo                 sc.Description = "Stream Dock Pro - Servidor Rust y Editor de PC";
echo                 sc.Save^(^);
echo             }
echo             catch { }
echo         }
echo.
echo         [STAThread]
echo         public static void Main^(^)
echo         {
echo             Application.EnableVisualStyles^(^);
echo             Application.SetCompatibleTextRenderingDefault^(false^);
echo             Application.Run^(new InstallerForm^(^)^);
echo         }
echo     }
echo }
) > "%INSTALLER_CS%"

"%CSC_EXE%" /nologo /target:winexe /optimize+ ^
    /win32icon:"%ICON_PATH%" ^
    /out:"%DIST_DIR%\StreamDockPro_Instalador.exe" ^
    /resource:"%DIST_DIR%\StreamDockPro.exe",StreamDockProPayload.exe ^
    /r:System.dll /r:System.Drawing.dll /r:System.Windows.Forms.dll /r:Microsoft.CSharp.dll ^
    "%INSTALLER_CS%"

rmdir /S /Q "%BUILD_TMP%" >nul 2>&1

for /f "usebackq delims=" %%D in (`powershell -NoProfile -Command "[Environment]::GetFolderPath('Desktop')"`) do set "DESKTOP_DIR=%%D"
if exist "%DIST_DIR%\StreamDockPro_Instalador.exe" (
    copy /Y "%DIST_DIR%\StreamDockPro_Instalador.exe" "!DESKTOP_DIR!\StreamDockPro_Instalador.exe" >nul
)

echo.
echo ========================================================
echo  [OK] ICONO, EJECUTABLE E INSTALADOR CREADOS CON EXITO
echo ========================================================
echo  1. Icono Oficial (.ICO)      : "%ICON_PATH%"
echo  2. Instalador en 1 solo .EXE : "%DIST_DIR%\StreamDockPro_Instalador.exe"
echo  3. Copia en el Escritorio    : "!DESKTOP_DIR!\StreamDockPro_Instalador.exe"
echo  4. Ejecutable portable unico : "%DIST_DIR%\StreamDockPro.exe"
echo ========================================================
echo.
pause
