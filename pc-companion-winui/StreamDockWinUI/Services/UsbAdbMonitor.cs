using System;
using System.Diagnostics;
using System.Threading;
using System.Threading.Tasks;

namespace StreamDockWinUI.Services
{
    public class UsbAdbMonitor
    {
        private CancellationTokenSource? _cts;

        public void StartUsbAdbMonitor(int pollIntervalMs = 5000)
        {
            _cts = new CancellationTokenSource();
            Task.Run(() => PollDevicesLoopAsync(_cts.Token, pollIntervalMs));
        }

        public void Stop()
        {
            _cts?.Cancel();
        }

        private async Task PollDevicesLoopAsync(CancellationToken token, int pollIntervalMs)
        {
            while (!token.IsCancellationRequested)
            {
                try
                {
                    var processInfo = new ProcessStartInfo
                    {
                        FileName = "adb",
                        Arguments = "devices",
                        RedirectStandardOutput = true,
                        UseShellExecute = false,
                        CreateNoWindow = true
                    };

                    using var process = Process.Start(processInfo);
                    if (process != null)
                    {
                        var output = await process.StandardOutput.ReadToEndAsync();
                        var lines = output.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries);

                        foreach (var line in lines)
                        {
                            if (line.EndsWith("\tdevice"))
                            {
                                var serial = line.Split('\t')[0];
                                SetupAdbReverse(serial);
                            }
                        }
                    }
                }
                catch (Exception)
                {
                    // ADB might not be in PATH or no devices attached
                }

                await Task.Delay(pollIntervalMs, token);
            }
        }

        private void SetupAdbReverse(string serial)
        {
            try
            {
                var processInfo = new ProcessStartInfo
                {
                    FileName = "adb",
                    Arguments = $"-s {serial} reverse tcp:8081 tcp:8081",
                    CreateNoWindow = true,
                    UseShellExecute = false
                };
                Process.Start(processInfo);
            }
            catch (Exception)
            {
            }
        }
    }
}
