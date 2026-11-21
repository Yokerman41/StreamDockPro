using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using Microsoft.Win32;

namespace StreamDockWinUI.Services
{
    public static class SpotifyController
    {
        private const int WM_APPCOMMAND = 0x0319;
        private const int APPCOMMAND_MEDIA_NEXTTRACK = 11;
        private const int APPCOMMAND_MEDIA_PREVIOUSTRACK = 12;
        private const int APPCOMMAND_MEDIA_STOP = 13;
        private const int APPCOMMAND_MEDIA_PLAY_PAUSE = 14;
        private const int SW_RESTORE = 9;

        private delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

        [DllImport("user32.dll")]
        private static extern bool EnumWindows(EnumWindowsProc enumProc, IntPtr lParam);

        [DllImport("user32.dll")]
        private static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        private static extern int GetClassName(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        private static extern int GetWindowTextLength(IntPtr hWnd);

        [DllImport("user32.dll")]
        private static extern IntPtr SendMessage(IntPtr hWnd, int Msg, IntPtr wParam, IntPtr lParam);

        [DllImport("user32.dll")]
        private static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

        [DllImport("user32.dll")]
        private static extern bool SetForegroundWindow(IntPtr hWnd);

        /// <summary>
        /// Busca la ruta del ejecutable de Spotify si está instalado en la PC
        /// (instalador clásico o versión de Microsoft Store).
        /// </summary>
        public static string? FindSpotifyExecutablePath()
        {
            var candidates = new List<string>
            {
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Spotify", "Spotify.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Spotify", "Spotify.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Microsoft", "WindowsApps", "Spotify.exe")
            };

            foreach (var path in candidates)
            {
                if (File.Exists(path))
                    return path;
            }

            // Buscar en WindowsApps (Microsoft Store)
            try
            {
                var localPackages = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "Packages"
                );
                if (Directory.Exists(localPackages))
                {
                    var spotifyPkg = Directory.GetDirectories(localPackages, "SpotifyAB.SpotifyMusic_*").FirstOrDefault();
                    if (spotifyPkg != null)
                    {
                        var aliasExe = Path.Combine(
                            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                            "Microsoft",
                            "WindowsApps",
                            Path.GetFileName(spotifyPkg),
                            "Spotify.exe"
                        );
                        if (File.Exists(aliasExe))
                            return aliasExe;
                    }
                }
            }
            catch
            {
            }

            // Buscar en el Registro de Windows
            try
            {
                using var key = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Uninstall\Spotify");
                var installLoc = key?.GetValue("InstallLocation")?.ToString();
                if (!string.IsNullOrWhiteSpace(installLoc))
                {
                    var exe = Path.Combine(installLoc, "Spotify.exe");
                    if (File.Exists(exe))
                        return exe;
                }
            }
            catch
            {
            }

            // Verificar si hay un proceso de Spotify en ejecución actualmente
            try
            {
                var proc = Process.GetProcessesByName("Spotify").FirstOrDefault();
                if (proc != null)
                {
                    try
                    {
                        return proc.MainModule?.FileName ?? "Spotify.exe";
                    }
                    catch
                    {
                        return "Spotify.exe";
                    }
                }
            }
            catch
            {
            }

            return null;
        }

        public static bool IsSpotifyInstalled()
        {
            if (FindSpotifyExecutablePath() != null)
                return true;

            // Verificar si el protocolo URI spotify: está registrado en Windows
            try
            {
                using var key = Registry.ClassesRoot.OpenSubKey("spotify");
                if (key != null)
                    return true;
            }
            catch
            {
            }

            return false;
        }

        public static bool IsSpotifyAction(string? actionValue)
        {
            if (string.IsNullOrWhiteSpace(actionValue)) return false;
            return actionValue.StartsWith("SPOTIFY_", StringComparison.OrdinalIgnoreCase);
        }

        public static (bool Success, string Message) ExecuteAction(string actionValue)
        {
            var cmd = actionValue?.Trim().ToUpperInvariant() ?? string.Empty;

            if (cmd == "SPOTIFY_OPEN")
            {
                var hwnds = FindSpotifyWindows();
                if (hwnds.Count > 0)
                {
                    ShowWindow(hwnds[0], SW_RESTORE);
                    SetForegroundWindow(hwnds[0]);
                    return (true, "Ventana de Spotify enfocada");
                }
                return LaunchSpotifyProcess();
            }

            if (cmd == "SPOTIFY_VOL_UP")
            {
                return AdjustSpotifyVolume(0.08f);
            }

            if (cmd == "SPOTIFY_VOL_DOWN")
            {
                return AdjustSpotifyVolume(-0.08f);
            }

            if (cmd == "SPOTIFY_MUTE")
            {
                return ToggleSpotifyMute();
            }

            int appCommand = cmd switch
            {
                "SPOTIFY_PLAY_PAUSE" => APPCOMMAND_MEDIA_PLAY_PAUSE,
                "SPOTIFY_NEXT" => APPCOMMAND_MEDIA_NEXTTRACK,
                "SPOTIFY_PREV" => APPCOMMAND_MEDIA_PREVIOUSTRACK,
                "SPOTIFY_STOP" => APPCOMMAND_MEDIA_STOP,
                _ => 0
            };

            if (appCommand == 0)
                return (false, $"Comando de Spotify no reconocido: {cmd}");

            var targetWindows = FindSpotifyWindows();
            if (targetWindows.Count == 0)
            {
                // Si Spotify está cerrado, lo abrimos automáticamente
                var launched = LaunchSpotifyProcess();
                return (launched.Success, launched.Success ? "Spotify no estaba abierto; iniciando Spotify..." : launched.Message);
            }

            IntPtr lParam = new IntPtr(appCommand << 16);
            foreach (var hwnd in targetWindows)
            {
                SendMessage(hwnd, WM_APPCOMMAND, hwnd, lParam);
            }

            return (true, $"Comando exclusivo enviado a Spotify ({cmd})");
        }

        private static (bool Success, string Message) LaunchSpotifyProcess()
        {
            try
            {
                var exePath = FindSpotifyExecutablePath();
                if (!string.IsNullOrWhiteSpace(exePath) && File.Exists(exePath))
                {
                    Process.Start(new ProcessStartInfo
                    {
                        FileName = exePath,
                        UseShellExecute = true
                    });
                    return (true, "Spotify iniciado");
                }

                Process.Start(new ProcessStartInfo
                {
                    FileName = "cmd.exe",
                    Arguments = "/C start \"\" \"spotify:\"",
                    CreateNoWindow = true,
                    UseShellExecute = false
                });
                return (true, "Spotify iniciado mediante protocolo URI");
            }
            catch (Exception ex)
            {
                return (false, $"No se pudo abrir Spotify: {ex.Message}");
            }
        }

        private static List<IntPtr> FindSpotifyWindows()
        {
            var result = new List<IntPtr>();
            var spotifyPids = new HashSet<uint>();

            try
            {
                foreach (var p in Process.GetProcessesByName("Spotify"))
                {
                    spotifyPids.Add((uint)p.Id);
                    if (p.MainWindowHandle != IntPtr.Zero)
                    {
                        result.Add(p.MainWindowHandle);
                    }
                }
            }
            catch
            {
            }

            if (spotifyPids.Count == 0)
                return result;

            EnumWindows((hWnd, lParam) =>
            {
                GetWindowThreadProcessId(hWnd, out uint pid);
                if (spotifyPids.Contains(pid))
                {
                    var sb = new StringBuilder(128);
                    GetClassName(hWnd, sb, sb.Capacity);
                    var className = sb.ToString();
                    if (className.StartsWith("Chrome_WidgetWin_", StringComparison.OrdinalIgnoreCase) &&
                        GetWindowTextLength(hWnd) > 0 &&
                        !result.Contains(hWnd))
                    {
                        result.Add(hWnd);
                    }
                }
                return true;
            }, IntPtr.Zero);

            return result;
        }

        // ====================================================================
        // Control de Volumen Exclusivo de Spotify en el Mezclador de Windows
        // ====================================================================
        private static (bool Success, string Message) AdjustSpotifyVolume(float delta)
        {
            try
            {
                var volumes = GetSpotifyAudioVolumes();
                if (volumes.Count == 0)
                    return (false, "Spotify no tiene una sesión de audio activa (reproduce una canción primero)");

                float newVol = 1.0f;
                foreach (var vol in volumes)
                {
                    vol.GetMasterVolume(out float current);
                    newVol = Math.Clamp(current + delta, 0.0f, 1.0f);
                    vol.SetMasterVolume(newVol, Guid.Empty);
                    vol.SetMute(false, Guid.Empty);
                }

                return (true, $"Volumen de Spotify ajustado al {(int)Math.Round(newVol * 100)}%");
            }
            catch (Exception ex)
            {
                return (false, $"Error ajustando volumen de Spotify: {ex.Message}");
            }
        }

        private static (bool Success, string Message) ToggleSpotifyMute()
        {
            try
            {
                var volumes = GetSpotifyAudioVolumes();
                if (volumes.Count == 0)
                    return (false, "Spotify no tiene una sesión de audio activa");

                bool targetMute = true;
                for (int i = 0; i < volumes.Count; i++)
                {
                    if (i == 0)
                    {
                        volumes[i].GetMute(out bool currentMute);
                        targetMute = !currentMute;
                    }
                    volumes[i].SetMute(targetMute, Guid.Empty);
                }

                return (true, targetMute ? "Spotify silenciado" : "Audio de Spotify activado");
            }
            catch (Exception ex)
            {
                return (false, $"Error silenciando Spotify: {ex.Message}");
            }
        }

        private static List<ISimpleAudioVolume> GetSpotifyAudioVolumes()
        {
            var result = new List<ISimpleAudioVolume>();
            var spotifyPids = new HashSet<uint>(
                Process.GetProcessesByName("Spotify").Select(p => (uint)p.Id)
            );
            if (spotifyPids.Count == 0)
                return result;

            var enumerator = (IMMDeviceEnumerator)new MMDeviceEnumeratorComObject();
            enumerator.GetDefaultAudioEndpoint(0 /* eRender */, 1 /* eMultimedia */, out IMMDevice device);
            if (device == null) return result;

            Guid iidSessionManager2 = typeof(IAudioSessionManager2).GUID;
            device.Activate(ref iidSessionManager2, 0, IntPtr.Zero, out object objMgr);
            if (objMgr is not IAudioSessionManager2 mgr) return result;

            mgr.GetSessionEnumerator(out IAudioSessionEnumerator sessionEnum);
            if (sessionEnum == null) return result;

            sessionEnum.GetCount(out int count);
            for (int i = 0; i < count; i++)
            {
                sessionEnum.GetSession(i, out IAudioSessionControl ctl);
                if (ctl is IAudioSessionControl2 ctl2)
                {
                    ctl2.GetProcessId(out uint pid);
                    if (spotifyPids.Contains(pid) && ctl is ISimpleAudioVolume simpleVol)
                    {
                        result.Add(simpleVol);
                    }
                }
            }

            return result;
        }

        [ComImport]
        [Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")]
        private class MMDeviceEnumeratorComObject
        {
        }

        [ComImport]
        [Guid("A95664D2-9614-4F35-A746-DE8DB63617E6")]
        [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IMMDeviceEnumerator
        {
            int NotImpl1();
            [PreserveSig]
            int GetDefaultAudioEndpoint(int dataFlow, int role, out IMMDevice ppDevice);
        }

        [ComImport]
        [Guid("D666063F-1587-4E43-81F1-B948E807363F")]
        [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IMMDevice
        {
            [PreserveSig]
            int Activate(ref Guid iid, int dwClsCtx, IntPtr pActivationParams, [MarshalAs(UnmanagedType.IUnknown)] out object ppInterface);
        }

        [ComImport]
        [Guid("77AA99A0-1BD6-484F-8BC7-2C654C9A9B6F")]
        [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IAudioSessionManager2
        {
            int NotImpl1();
            int NotImpl2();
            [PreserveSig]
            int GetSessionEnumerator(out IAudioSessionEnumerator SessionEnum);
        }

        [ComImport]
        [Guid("E2F5BB11-0570-40CA-ACDD-3AA01277DEE8")]
        [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IAudioSessionEnumerator
        {
            [PreserveSig]
            int GetCount(out int SessionCount);
            [PreserveSig]
            int GetSession(int SessionCount, out IAudioSessionControl Session);
        }

        [ComImport]
        [Guid("F4B1A599-7266-4319-A8CA-E70ACB11E8CD")]
        [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IAudioSessionControl
        {
        }

        [ComImport]
        [Guid("bfb7ff88-7239-4fc9-8fa2-07c950be9c6d")]
        [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IAudioSessionControl2
        {
            int NotImpl1();
            int NotImpl2();
            int NotImpl3();
            int NotImpl4();
            int NotImpl5();
            int NotImpl6();
            int NotImpl7();
            int NotImpl8();
            int NotImpl9();
            [PreserveSig]
            int GetSessionIdentifier([MarshalAs(UnmanagedType.LPWStr)] out string pRetVal);
            [PreserveSig]
            int GetSessionInstanceIdentifier([MarshalAs(UnmanagedType.LPWStr)] out string pRetVal);
            [PreserveSig]
            int GetProcessId(out uint pRetVal);
        }

        [ComImport]
        [Guid("87CE5498-68D6-44E5-9215-6DA47EF883D8")]
        [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface ISimpleAudioVolume
        {
            [PreserveSig]
            int SetMasterVolume(float fLevel, Guid EventContext);
            [PreserveSig]
            int GetMasterVolume(out float pfLevel);
            [PreserveSig]
            int SetMute(bool bMute, Guid EventContext);
            [PreserveSig]
            int GetMute(out bool pbMute);
        }
    }
}
