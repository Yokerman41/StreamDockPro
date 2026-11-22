using System;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;

namespace StreamDockWinUI.Services
{
    public static class WindowController
    {
        private const int SW_MAXIMIZE = 3;
        private const int SW_MINIMIZE = 6;
        private const int SW_RESTORE = 9;
        private const uint WM_CLOSE = 0x0010;

        private delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

        [DllImport("user32.dll")]
        private static extern IntPtr GetForegroundWindow();

        [DllImport("user32.dll")]
        private static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);

        [DllImport("user32.dll")]
        private static extern bool IsWindowVisible(IntPtr hWnd);

        [DllImport("user32.dll")]
        private static extern bool IsZoomed(IntPtr hWnd);

        [DllImport("user32.dll")]
        private static extern bool IsIconic(IntPtr hWnd);

        [DllImport("user32.dll")]
        private static extern bool ShowWindowAsync(IntPtr hWnd, int nCmdShow);

        [DllImport("user32.dll")]
        private static extern bool PostMessage(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        private static extern int GetClassName(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        private static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        private static extern int GetWindowTextLength(IntPtr hWnd);

        [DllImport("user32.dll")]
        private static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

        public static string? GetForegroundProcessName()
        {
            try
            {
                IntPtr fg = GetForegroundWindow();
                if (fg == IntPtr.Zero) return null;
                GetWindowThreadProcessId(fg, out uint pid);
                if (pid == 0 || pid == (uint)Environment.ProcessId) return null;
                using var proc = Process.GetProcessById((int)pid);
                return proc.ProcessName;
            }
            catch
            {
                return null;
            }
        }

        public static bool IsWindowAction(string? actionValue)
        {
            if (string.IsNullOrWhiteSpace(actionValue)) return false;
            var val = actionValue.Trim();
            return val.StartsWith("WIN_MINIMIZE", StringComparison.OrdinalIgnoreCase)
                || val.StartsWith("WIN_MAXIMIZE", StringComparison.OrdinalIgnoreCase)
                || val.StartsWith("WIN_CLOSE", StringComparison.OrdinalIgnoreCase);
        }

        public static (bool Success, string Message) ExecuteWindowAction(string actionValue)
        {
            var raw = actionValue?.Trim() ?? string.Empty;
            string commandPart = raw;
            string? targetApp = null;

            int colonIdx = raw.IndexOf(':');
            if (colonIdx > 0)
            {
                commandPart = raw.Substring(0, colonIdx).Trim().ToUpperInvariant();
                targetApp = raw.Substring(colonIdx + 1).Trim();
            }
            else
            {
                commandPart = raw.ToUpperInvariant();
            }

            IntPtr targetHwnd = !string.IsNullOrWhiteSpace(targetApp)
                ? FindWindowForApp(targetApp)
                : FindTargetActiveWindow();

            if (targetHwnd == IntPtr.Zero)
            {
                return (false, "No se encontró ninguna ventana activa disponible");
            }

            string winTitle = GetTitle(targetHwnd);

            switch (commandPart)
            {
                case "WIN_MINIMIZE":
                    ShowWindowAsync(targetHwnd, SW_MINIMIZE);
                    return (true, $"Ventana minimizada: '{winTitle}'");

                case "WIN_MAXIMIZE":
                    if (IsZoomed(targetHwnd))
                    {
                        ShowWindowAsync(targetHwnd, SW_RESTORE);
                        return (true, $"Ventana restaurada: '{winTitle}'");
                    }
                    else
                    {
                        ShowWindowAsync(targetHwnd, SW_MAXIMIZE);
                        return (true, $"Ventana maximizada: '{winTitle}'");
                    }

                case "WIN_CLOSE":
                    PostMessage(targetHwnd, WM_CLOSE, IntPtr.Zero, IntPtr.Zero);
                    return (true, $"Cerrando ventana: '{winTitle}'");

                default:
                    return (false, $"Acción de ventana desconocida: {commandPart}");
            }
        }

        private static IntPtr FindTargetActiveWindow()
        {
            uint currentPid = (uint)Environment.ProcessId;
            IntPtr fg = GetForegroundWindow();

            if (fg != IntPtr.Zero && IsControllableUserWindow(fg, currentPid))
            {
                return fg;
            }

            IntPtr found = IntPtr.Zero;
            EnumWindows((hWnd, lParam) =>
            {
                if (IsControllableUserWindow(hWnd, currentPid) && !IsIconic(hWnd))
                {
                    found = hWnd;
                    return false;
                }
                return true;
            }, IntPtr.Zero);

            if (found == IntPtr.Zero)
            {
                EnumWindows((hWnd, lParam) =>
                {
                    if (IsControllableUserWindow(hWnd, currentPid))
                    {
                        found = hWnd;
                        return false;
                    }
                    return true;
                }, IntPtr.Zero);
            }

            return found;
        }

        private static IntPtr FindWindowForApp(string appPathOrName)
        {
            var procName = Path.GetFileNameWithoutExtension(appPathOrName.Trim('"'));
            if (string.IsNullOrWhiteSpace(procName))
                return FindTargetActiveWindow();

            IntPtr found = IntPtr.Zero;
            try
            {
                foreach (var p in Process.GetProcessesByName(procName))
                {
                    if (p.MainWindowHandle != IntPtr.Zero)
                    {
                        return p.MainWindowHandle;
                    }
                }
            }
            catch
            {
            }

            return found != IntPtr.Zero ? found : FindTargetActiveWindow();
        }

        private static bool IsControllableUserWindow(IntPtr hWnd, uint excludePid)
        {
            if (!IsWindowVisible(hWnd))
                return false;

            if (GetWindowTextLength(hWnd) <= 0)
                return false;

            GetWindowThreadProcessId(hWnd, out uint pid);
            if (pid == excludePid)
                return false;

            var sbClass = new StringBuilder(128);
            GetClassName(hWnd, sbClass, sbClass.Capacity);
            var cls = sbClass.ToString();

            if (cls.Equals("Progman", StringComparison.OrdinalIgnoreCase) ||
                cls.Equals("WorkerW", StringComparison.OrdinalIgnoreCase) ||
                cls.Equals("Shell_TrayWnd", StringComparison.OrdinalIgnoreCase) ||
                cls.Equals("Shell_SecondaryTrayWnd", StringComparison.OrdinalIgnoreCase) ||
                cls.Equals("Windows.UI.Core.CoreWindow", StringComparison.OrdinalIgnoreCase))
            {
                return false;
            }

            var title = GetTitle(hWnd);
            if (title.Equals("Program Manager", StringComparison.OrdinalIgnoreCase) ||
                title.Equals("Windows Input Experience", StringComparison.OrdinalIgnoreCase))
            {
                return false;
            }

            return true;
        }

        private static string GetTitle(IntPtr hWnd)
        {
            var sb = new StringBuilder(256);
            GetWindowText(hWnd, sb, sb.Capacity);
            return sb.ToString();
        }
    }
}
