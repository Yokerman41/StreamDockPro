using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;
using StreamDockWinUI.Services;
using StreamDockWinUI.ViewModels;

namespace StreamDockWinUI
{
    public partial class MainWindow : Window
    {
        private CancellationTokenSource? _holdCts;
        private bool _moveTriggeredOnCurrentHold;

        public MainWindow()
        {
            InitializeComponent();
        }

        private async void Button_PreviewMouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            _holdCts?.Cancel();
            _holdCts = new CancellationTokenSource();
            _moveTriggeredOnCurrentHold = false;
            var token = _holdCts.Token;

            if (sender is FrameworkElement element && element.DataContext is ButtonModel buttonModel)
            {
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

        private void Button_PreviewMouseLeftButtonUp(object sender, MouseButtonEventArgs e)
        {
            _holdCts?.Cancel();
            _holdCts = null;

            if (_moveTriggeredOnCurrentHold)
            {
                // Consumir el MouseUp que activó el modo mover para esperar al clic en el botón destino
                _moveTriggeredOnCurrentHold = false;
                e.Handled = true;
            }
        }

        private void Button_PreviewDragOver(object sender, DragEventArgs e)
        {
            if (e.Data.GetDataPresent(DataFormats.FileDrop))
            {
                e.Effects = DragDropEffects.Copy;
            }
            else
            {
                e.Effects = DragDropEffects.None;
            }
            e.Handled = true;
        }

        private void Button_Drop(object sender, DragEventArgs e)
        {
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
                            _ = mainViewModel.SaveButtonConfiguration();
                            mainViewModel.StatusMessage = $"✓ Botón #{buttonModel.Position + 1} configurado con icono e imagen: '{fileName}'";
                        }
                    }
                }
            }
        }
    }
}
