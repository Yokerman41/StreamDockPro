using System;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Windows;
using StreamDockWinUI.Services;

namespace StreamDockWinUI
{
    public partial class App : Application
    {
        private UsbAdbMonitor? _adbMonitor;

        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);
            EnsureRustServerRunning();

            _adbMonitor = new UsbAdbMonitor();
            _adbMonitor.StartUsbAdbMonitor();
        }

        protected override void OnExit(ExitEventArgs e)
        {
            _adbMonitor?.Stop();
            base.OnExit(e);
        }

        private static void EnsureRustServerRunning()
        {
            try
            {
                var extractedServerPath = ExtractEmbeddedRustServerIfNeeded();

                if (Process.GetProcessesByName("pc-companion-rust").Any())
                {
                    return;
                }

                var baseDir = AppDomain.CurrentDomain.BaseDirectory;
                var candidatePaths = new[]
                {
                    extractedServerPath,
                    Path.Combine(baseDir, "pc-companion-rust.exe"),
                    Path.GetFullPath(Path.Combine(baseDir, @"..\..\..\..\..\pc-companion-rust\target\release\pc-companion-rust.exe")),
                    Path.GetFullPath(Path.Combine(baseDir, @"..\..\..\..\..\pc-companion-rust\target\debug\pc-companion-rust.exe"))
                };

                var serverExe = candidatePaths.FirstOrDefault(p => !string.IsNullOrEmpty(p) && File.Exists(p));
                if (!string.IsNullOrEmpty(serverExe))
                {
                    Process.Start(new ProcessStartInfo
                    {
                        FileName = serverExe,
                        WorkingDirectory = Path.GetDirectoryName(serverExe) ?? baseDir,
                        CreateNoWindow = true,
                        UseShellExecute = false,
                        WindowStyle = ProcessWindowStyle.Hidden
                    });
                }
            }
            catch
            {
                // Ignore if server cannot be auto-started
            }
        }

        private static string? ExtractEmbeddedRustServerIfNeeded()
        {
            try
            {
                var assembly = Assembly.GetExecutingAssembly();
                using var stream = assembly.GetManifestResourceStream("pc-companion-rust.exe");
                if (stream == null)
                {
                    return null;
                }

                var serverDir = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "StreamDockPro",
                    "server"
                );
                Directory.CreateDirectory(serverDir);
                var targetExe = Path.Combine(serverDir, "pc-companion-rust.exe");

                bool shouldWrite = !File.Exists(targetExe) || new FileInfo(targetExe).Length != stream.Length;
                if (shouldWrite)
                {
                    try
                    {
                        foreach (var proc in Process.GetProcessesByName("pc-companion-rust"))
                        {
                            try { proc.Kill(); proc.WaitForExit(1000); } catch { }
                        }
                        using var fs = new FileStream(targetExe, FileMode.Create, FileAccess.Write, FileShare.None);
                        stream.CopyTo(fs);
                    }
                    catch
                    {
                        // If locked by running process, existing binary is fine
                    }
                }

                return targetExe;
            }
            catch
            {
                return null;
            }
        }
    }
}
