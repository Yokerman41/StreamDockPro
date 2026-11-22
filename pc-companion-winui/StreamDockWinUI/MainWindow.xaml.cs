using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Drawing;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;
using StreamDockWinUI.ViewModels;
using Forms = System.Windows.Forms;
using DragDropEffects = System.Windows.DragDropEffects;
using DragEventArgs = System.Windows.DragEventArgs;
using DataFormats = System.Windows.DataFormats;
using DataObject = System.Windows.DataObject;
using Point = System.Windows.Point;

namespace StreamDockWinUI
{
    public partial class MainWindow : Window
    {
        private CancellationTokenSource? _holdCts;
        private bool _moveTriggeredOnCurrentHold;
        private Point? _dragStartPoint;
        private ButtonModel? _draggedButton;
        private Forms.NotifyIcon? _notifyIcon;
        private bool _isExplicitExit;

        public MainWindow()
        {
            InitializeComponent();
            InitializeSystemTray();
            PreviewKeyDown += MainWindow_PreviewKeyDown;
        }

        private void InitializeSystemTray()
        {
            try
            {
                System.Drawing.Icon? trayIcon = null;
                var iconPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "app_icon.ico");
                if (File.Exists(iconPath))
                {
                    trayIcon = new System.Drawing.Icon(iconPath);
                }
                else
                {
                    var exePath = Environment.ProcessPath;
                    if (!string.IsNullOrEmpty(exePath) && File.Exists(exePath))
                    {
                        trayIcon = System.Drawing.Icon.ExtractAssociatedIcon(exePath);
                    }
                }

                var contextMenu = new Forms.ContextMenuStrip();
                var openItem = new Forms.ToolStripMenuItem("Abrir Stream Dock Pro");
                openItem.Font = new Font(openItem.Font, System.Drawing.FontStyle.Bold);
                openItem.Click += (_, _) => RestoreFromTray();

                var exitItem = new Forms.ToolStripMenuItem("Salir completamente (Cerrar Servidor)");
                exitItem.Click += (_, _) =>
                {
                    _isExplicitExit = true;
                    _notifyIcon?.Dispose();
                    System.Windows.Application.Current.Shutdown();
                };

                contextMenu.Items.Add(openItem);
                contextMenu.Items.Add(new Forms.ToolStripSeparator());
                contextMenu.Items.Add(exitItem);

                _notifyIcon = new Forms.NotifyIcon
                {
                    Icon = trayIcon ?? SystemIcons.Application,
                    Text = "Stream Dock Pro (Servidor Activo)",
                    Visible = true,
                    ContextMenuStrip = contextMenu
                };
                _notifyIcon.DoubleClick += (_, _) => RestoreFromTray();
            }
            catch
            {
            }
        }

        private void RestoreFromTray()
        {
            Show();
            WindowState = WindowState.Normal;
            Activate();
        }

        private void MinimizeToTray_Click(object sender, RoutedEventArgs e)
        {
            Hide();
            _notifyIcon?.ShowBalloonTip(
                2000,
                "Stream Dock Pro sigue activo",
                "El servidor sigue conectado a tu teléfono en la bandeja del sistema.",
                Forms.ToolTipIcon.Info
            );
        }

        protected override void OnClosing(CancelEventArgs e)
        {
            if (!_isExplicitExit && _notifyIcon != null)
            {
                e.Cancel = true;
                Hide();
                _notifyIcon.ShowBalloonTip(
                    2000,
                    "Stream Dock Pro en segundo plano",
                    "Minimizado a la bandeja del reloj para mantener activo tu teléfono. Clic derecho en el icono para salir.",
                    Forms.ToolTipIcon.Info
                );
                return;
            }

            _notifyIcon?.Dispose();
            base.OnClosing(e);
        }

        private void MainWindow_PreviewKeyDown(object sender, System.Windows.Input.KeyEventArgs e)
        {
            if (DataContext is not MainViewModel vm || !vm.IsRecordingHotkey)
                return;

            e.Handled = true;
            var key = e.Key == Key.System ? e.SystemKey : e.Key;

            if (key == Key.Escape)
            {
                vm.ToggleHotkeyRecording();
                return;
            }

            // Ignorar si solo se ha presionado una tecla modificadora esperando la tecla final
            if (key == Key.LeftCtrl || key == Key.RightCtrl ||
                key == Key.LeftAlt || key == Key.RightAlt ||
                key == Key.LeftShift || key == Key.RightShift ||
                key == Key.LWin || key == Key.RWin)
            {
                return;
            }

            var parts = new List<string>();
            var mods = Keyboard.Modifiers;
            if (mods.HasFlag(ModifierKeys.Control)) parts.Add("CTRL");
            if (mods.HasFlag(ModifierKeys.Alt)) parts.Add("ALT");
            if (mods.HasFlag(ModifierKeys.Shift)) parts.Add("SHIFT");
            if (mods.HasFlag(ModifierKeys.Windows)) parts.Add("WIN");

            string keyName = key switch
            {
                >= Key.D0 and <= Key.D9 => ((char)('0' + (key - Key.D0))).ToString(),
                >= Key.NumPad0 and <= Key.NumPad9 => ((char)('0' + (key - Key.NumPad0))).ToString(),
                Key.Return => "ENTER",
                Key.Back => "BACKSPACE",
                Key.Space => "SPACE",
                Key.Prior => "PAGEUP",
                Key.Next => "PAGEDOWN",
                _ => key.ToString().ToUpperInvariant()
            };

            parts.Add(keyName);
            vm.ApplyRecordedHotkey(string.Join("+", parts));
        }

        private async void Button_PreviewMouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            _dragStartPoint = e.GetPosition(null);
            _holdCts?.Cancel();
            _holdCts = new CancellationTokenSource();
            _moveTriggeredOnCurrentHold = false;
            var token = _holdCts.Token;

            if (sender is FrameworkElement element && element.DataContext is ButtonModel buttonModel)
            {
                _draggedButton = buttonModel;
                try
                {
                    await Task.Delay(6000, token);
                    if (!token.IsCancellationRequested && DataContext is MainViewModel mainViewModel)
                    {
                        _moveTriggeredOnCurrentHold = true;
                        mainViewModel.StartMoveButton(buttonModel);
                    }
                }
                catch (TaskCanceledException)
                {
                }
            }
        }

        private void Button_PreviewMouseMove(object sender, System.Windows.Input.MouseEventArgs e)
        {
            if (e.LeftButton == MouseButtonState.Pressed && _dragStartPoint.HasValue && _draggedButton != null)
            {
                Point currentPos = e.GetPosition(null);
                Vector diff = _dragStartPoint.Value - currentPos;

                if (Math.Abs(diff.X) > SystemParameters.MinimumHorizontalDragDistance ||
                    Math.Abs(diff.Y) > SystemParameters.MinimumVerticalDragDistance)
                {
                    _holdCts?.Cancel();
                    var btnToDrag = _draggedButton;
                    _dragStartPoint = null;
                    _draggedButton = null;

                    if (sender is DependencyObject depObj)
                    {
                        var data = new DataObject("StreamDockButton", btnToDrag);
                        DragDrop.DoDragDrop(depObj, data, DragDropEffects.Move);
                    }
                }
            }
        }

        private void Button_PreviewMouseLeftButtonUp(object sender, MouseButtonEventArgs e)
        {
            _dragStartPoint = null;
            _draggedButton = null;
            _holdCts?.Cancel();
            _holdCts = null;

            if (_moveTriggeredOnCurrentHold)
            {
                _moveTriggeredOnCurrentHold = false;
                e.Handled = true;
            }
        }

        private void Button_PreviewDragOver(object sender, DragEventArgs e)
        {
            if (e.Data.GetDataPresent("StreamDockButton"))
            {
                e.Effects = DragDropEffects.Move;
            }
            else if (e.Data.GetDataPresent(DataFormats.FileDrop))
            {
                e.Effects = DragDropEffects.Copy;
            }
            else
            {
                e.Effects = DragDropEffects.None;
            }
            e.Handled = true;
        }

        private async void Button_Drop(object sender, DragEventArgs e)
        {
            if (e.Data.GetDataPresent("StreamDockButton"))
            {
                if (e.Data.GetData("StreamDockButton") is ButtonModel sourceBtn &&
                    sender is FrameworkElement targetEl &&
                    targetEl.DataContext is ButtonModel targetBtn &&
                    DataContext is MainViewModel vm)
                {
                    await vm.SwapButtonsDirectAsync(sourceBtn, targetBtn);
                }
                return;
            }

            if (e.Data.GetDataPresent(DataFormats.FileDrop))
            {
                var files = (string[])e.Data.GetData(DataFormats.FileDrop);
                if (files != null && files.Length > 0)
                {
                    var filePath = files[0];
                    var fileName = Path.GetFileNameWithoutExtension(filePath);

                    if (sender is FrameworkElement element && element.DataContext is ButtonModel buttonModel)
                    {
                        buttonModel.Label = fileName;
                        buttonModel.ActionType = "LAUNCH_APP";
                        buttonModel.ActionValue = filePath;
                        buttonModel.BackgroundColor = "#2A2A3C";
                        buttonModel.TryExtractIcon(filePath);

                        if (DataContext is MainViewModel mainViewModel)
                        {
                            mainViewModel.SelectButton(buttonModel);
                            await mainViewModel.SaveButtonConfiguration();
                            mainViewModel.StatusMessage = $"✓ Botón #{buttonModel.Position + 1} configurado con icono e imagen: '{fileName}'";
                        }
                    }
                }
            }
        }
    }
}
